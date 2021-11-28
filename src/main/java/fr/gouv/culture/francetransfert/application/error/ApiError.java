package fr.gouv.culture.francetransfert.application.error;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The type Api error.
 * 
 * @author Open Group
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class ApiError {
	/**
	 * Http Status Code
	 */
	private int statusCode;
	/**
	 * TYPE ERROR
	 */
	private String Type;
	/**
	 * ID ERROR
	 */
	private String id;
}
