package org.cdpg.dx.common.util;

import org.cdpg.dx.auth.authentication.exception.AuthenticationException;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.exception.*;

public class ExceptionHttpStatusMapper {

    public static HttpStatusCode map(Throwable throwable) {
        return switch (throwable) {
            case NoRowFoundException e -> HttpStatusCode.NOT_FOUND;
            case InvalidColumnNameException e -> HttpStatusCode.BAD_REQUEST;
            case UniqueConstraintViolationException e -> HttpStatusCode.CONFLICT;
            case DxPgException e -> HttpStatusCode.INTERNAL_SERVER_ERROR;
            case AuthenticationException e-> HttpStatusCode.UNAUTHORIZED;
            case DxUnauthorizedException e-> HttpStatusCode.UNAUTHORIZED;
            case DxForbiddenException e-> HttpStatusCode.FORBIDDEN;
            case BaseDxException e -> HttpStatusCode.BAD_REQUEST;
            default -> HttpStatusCode.INTERNAL_SERVER_ERROR;
        };
    }
}
