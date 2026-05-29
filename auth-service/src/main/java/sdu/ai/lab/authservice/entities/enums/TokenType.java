package sdu.ai.lab.authservice.entities.enums;

// todo: отрефакторить все использования toString(), исползовагние tOString как значение очень плохо лучше геттеры
public enum TokenType {
    accessToken, refreshToken;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}