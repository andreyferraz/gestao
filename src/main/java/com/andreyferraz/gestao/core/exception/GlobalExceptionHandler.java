package com.andreyferraz.gestao.core.exception;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ApiError> handleNoSuchElementException(NoSuchElementException ex, HttpServletRequest request) {
		return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
		return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getAllErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Dados da requisicao invalidos.");
		return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		String parameterName = ex.getName();
		Object value = ex.getValue();
		Class<?> expectedType = ex.getRequiredType();
		String expectedTypeName = expectedType != null ? expectedType.getSimpleName() : "tipo esperado";
		String message = String.format("Valor '%s' invalido para o parametro '%s'. Esperado: %s.",
				value, parameterName, expectedTypeName);
		return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		String message = "Corpo da requisicao invalido. Verifique tipos de campos, UUID e valores de enum.";
		return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiError> handleMissingServletRequestParameterException(
			MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		String message = String.format("Parametro obrigatorio ausente: '%s'.", ex.getParameterName());
		return buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
		log.error("Erro interno ao processar [{} {}]", request.getMethod(), request.getRequestURI(), ex);
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.", request.getRequestURI());
	}

	private ResponseEntity<ApiError> buildError(HttpStatus status, String message, String path) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path);
		return ResponseEntity.status(status).body(error);
	}

	public record ApiError(
			LocalDateTime timestamp,
			int status,
			String error,
			String message,
			String path) {
	}
}