/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.anfc.lmos.wot.binding


/**
 * This exception is thrown when the a [ProtocolClient] implementation does not support a
 * requested functionality.
 */
class ProtocolClientNotImplementedException : ProtocolClientException {
    constructor(
        clazz: Class<*>,
        operation: String
    ) : super(clazz.getSimpleName() + " does not implement '" + operation + "'")

    constructor(message: String) : super(message)
}
