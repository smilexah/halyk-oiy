package sdu.ai.lab.authservice.config.interceptors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnknownParametersInterceptor implements HandlerInterceptor {
    
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                            @NonNull HttpServletResponse response, 
                            @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            return true;
        }

        Parameter[] parameters = handlerMethod.getMethod().getParameters();
        Set<String> validParamNames = new HashSet<>();
        
        // Стандартные параметры пагинации
        validParamNames.addAll(Arrays.asList("page", "size", "sortBy", "sortDirection", "sort"));
        
        // Параметры для фильтрации по датам и типам
        validParamNames.addAll(Arrays.asList("dateFrom", "dateTo", "type", "start", "end", "startDate", "endDate"));

        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(ModelAttribute.class)) {
                Class<?> paramType = parameter.getType();
                validParamNames.addAll(getFieldNames(paramType));
            }
        }

        Map<String, String[]> requestParams = request.getParameterMap();
        Set<String> unknownParams = new HashSet<>();
        
        for (String paramName : requestParams.keySet()) {
            if (!validParamNames.contains(paramName)) {
                unknownParams.add(paramName);
            }
        }

        if (!unknownParams.isEmpty()) {
            log.warn("Unknown parameters in request: {}", unknownParams);
            sendErrorResponse(response, unknownParams);
            return false;
        }

        return true;
    }

    private Set<String> getFieldNames(Class<?> clazz) {
        Set<String> fieldNames = new HashSet<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                fieldNames.add(field.getName());
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fieldNames;
    }

    private void sendErrorResponse(HttpServletResponse response, Set<String> unknownParams) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String unknownParamsStr = String.join(", ", unknownParams);
        String message = messageSource.getMessage(
                "error.request.unknownParameters",
                new Object[]{unknownParamsStr},
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_BAD_REQUEST);
        errorResponse.put("error", messageSource.getMessage("error.badRequest", null, LocaleContextHolder.getLocale()));
        errorResponse.put("message", message);
        errorResponse.put("validationErrors", null);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
