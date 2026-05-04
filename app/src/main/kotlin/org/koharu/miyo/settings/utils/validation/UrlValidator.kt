package org.koharu.miyo.settings.utils.validation

import android.webkit.URLUtil
import org.koharu.miyo.R
import org.koharu.miyo.core.util.EditTextValidator

class UrlValidator : EditTextValidator() {

	override fun validate(text: String): ValidationResult {
		val trimmed = text.trim()
		if (trimmed.isEmpty()) {
			return ValidationResult.Success
		}
		return if (!isValidUrl(trimmed)) {
			ValidationResult.Failed(context.getString(R.string.invalid_server_address_message))
		} else {
			ValidationResult.Success
		}
	}

	private fun isValidUrl(str: String): Boolean {
		return URLUtil.isValidUrl(str) || DomainValidator.isValidDomain(str)
	}
}
