/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.thingweb.security

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Certificate-based asymmetric key security configuration conformant with X509V3 identified by the
 * term cert (i.e., "scheme": "cert").<br></br> See also: https://www.w3.org/2019/wot/security#certsecurityscheme
 */
class CertSecurityScheme(@field:JsonInclude(JsonInclude.Include.NON_EMPTY) val identity: String) : SecurityScheme {

    override fun toString(): String {
        return "CertSecurityScheme{" +
                "identity='" + identity + '\'' +
                '}'
    }
}
