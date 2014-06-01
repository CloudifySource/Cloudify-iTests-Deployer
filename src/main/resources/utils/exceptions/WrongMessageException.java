/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package utils.exceptions;

/**
 * Exception for various operation that expect a curtain message but get a different one.
 *
 * @author Eli Polonsky
 */
public class WrongMessageException extends Exception {

    private String actualMessage;
    private String expectedMessage;

    public WrongMessageException(final String actualMessage, final String expectedMessage) {
        super(message(actualMessage, expectedMessage));
        this.actualMessage = actualMessage;
        this.expectedMessage = expectedMessage;
    }

    public String getActualMessage() {
        return actualMessage;
    }

    public String getExpectedMessage() {
        return expectedMessage;
    }

    private static String message(final String actualMessage, final String expectedMessage) {
        return "Excepted message [" + expectedMessage + "] did not match actual message [" + actualMessage + "]";
    }
}
