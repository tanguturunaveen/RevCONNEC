package org.revature.revconnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        T safeData = data;
        if (safeData == null) {
            @SuppressWarnings("unchecked")
            T emptyObject = (T) Map.of();
            safeData = emptyObject;
        }
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(safeData)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        T safeData = data;
        if (safeData == null) {
            @SuppressWarnings("unchecked")
            T emptyObject = (T) Map.of();
            safeData = emptyObject;
        }
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(safeData)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
