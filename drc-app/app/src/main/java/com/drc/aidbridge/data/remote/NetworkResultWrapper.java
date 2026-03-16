package com.drc.aidbridge.data.remote;

/**
 * NetworkResultWrapper — a sealed-class-like wrapper for API call results.
 *
 * Represents three possible states of an asynchronous operation:
 * - Success: The operation completed and returned data.
 * - Error:   The operation failed with an error message.
 * - Loading: The operation is in progress.
 *
 * @param <T> The type of the successful result data.
 */
public abstract class NetworkResultWrapper<T> {

    private boolean hasBeenHandled = false;

    /** Operation is in progress. */
    public static class Loading<T> extends NetworkResultWrapper<T> {
    }

    /** Operation succeeded with result data. */
    public static class Success<T> extends NetworkResultWrapper<T> {
        public final T data;
        public Success(T data) {
            this.data = data;
        }
    }

    /** Operation failed with an error message. */
    public static class Error<T> extends NetworkResultWrapper<T> {
        public final String message;
        public final int code;
        public Error(String message, int code) {
            this.message = message;
            this.code = code;
        }
    }

    public boolean isLoading() {
        return this instanceof Loading;
    }

    public boolean isSuccess() {
        return this instanceof Success;
    }

    public boolean isError() {
        return this instanceof Error;
    }

    public T getData() {
        if (this instanceof Success) {
            return ((Success<T>) this).data;
        }
        return null;
    }

    public String getMessage() {
        if (this instanceof Error) {
            return ((Error<T>) this).message;
        }
        return null;
    }

    public boolean hasBeenHandled() {
        return hasBeenHandled;
    }

    public void markAsHandled() {
        this.hasBeenHandled = true;
    }

    public static <T> Loading<T> loading() {
        return new Loading<>();
    }

    public static <T> Success<T> success(T data) {
        return new Success<>(data);
    }

    public static <T> Error<T> error(String message) {
        return new Error<>(message, 0);
    }

    public static <T> Error<T> error(String message, int code) {
        return new Error<>(message, code);
    }
}
