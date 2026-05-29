package sdu.ai.lab.authservice.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValueConstants {
    public static final ZoneId ZONE_ID = ZoneId.of("UTC+05:00");
    public static final String USER_ID_CLAIM = "user_id";
    public static final String USER_NAME_CLAIM = "name";
    public static final String FILE_MANAGER_ACCOMMODATION_IMAGE_DIR = "accommodation-images";
//    public static final String FILE_MANAGER_ACCOMMODATION_DOCUMENT_DIR = "accommodation-documents";
    public static final String FILE_MANAGER_ACCOMMODATION_UNIT_IMAGE_DIR = "accommodation-unit-images";
    public static final String FILE_MANAGER_USER_PHOTOS_DIR = "user-photos";
}