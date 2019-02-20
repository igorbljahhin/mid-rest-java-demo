package ee.sk.middemo.exception;

/*-
 * #%L
 * Mobile ID sample Java client
 * %%
 * Copyright (C) 2018 - 2019 SK ID Solutions AS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.List;

import ee.sk.mid.exception.UserCancellationException;

public class MidAuthException extends RuntimeException {

    public MidAuthException(Exception e) {
        super(e);
    }

    public MidAuthException(UserCancellationException e) {

    }

    public MidAuthException(List<String> errors) {
        super("Invalid authentication. " + String.join(", ", errors));
    }

    public String getMessage() {
        return this.getCause().getMessage();
    }

}
