/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb.security

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

/**
 * Basic authentication security configuration identified by the term basic (i.e., "scheme":
 * "basic"), using an unencrypted username and password. This scheme should be used with some other
 * security mechanism providing confidentiality, for example, TLS.<br></br> See also:
 * https://www.w3.org/2019/wot/security#basicsecurityscheme
 */
class BasicSecurityScheme @JvmOverloads constructor(@field:JsonInclude(JsonInclude.Include.NON_EMPTY) var `in`: String? = null) :
    SecurityScheme {

    override fun hashCode(): Int {
        return Objects.hash(`in`)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is BasicSecurityScheme) {
            return false
        }
        return `in` == o.`in`
    }

    override fun toString(): String {
        return "BasicSecurityScheme{" +
                "in='" + `in` + '\'' +
                '}'
    }
}
