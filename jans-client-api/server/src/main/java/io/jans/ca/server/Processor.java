/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server;

import com.google.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.IParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.op.IOperation;
import io.jans.ca.server.op.OperationFactory;
import io.jans.ca.server.service.ValidationService;

/**
 * oxD operation processor.
 *
 * @author Yuriy Zabrovarnyy
 */
public class Processor {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    private final ValidationService validationService;

    @Inject
    public Processor(ValidationService validationService) {
        this.validationService = validationService;
    }

    public IOpResponse process(Command command) {
        if (command != null) {
            try {
                final IOperation<IParams> operation = (IOperation<IParams>) OperationFactory.create(command, ServerLauncher.getInjector());
                if (operation != null) {
                    IParams iParams = Convertor.asParams(operation.getParameterClass(), command);
                    validationService.validate(iParams);

                    IOpResponse operationResponse = operation.execute(iParams);
                    if (operationResponse != null) {
                        return operationResponse;
                    } else {
                        LOG.error("No response from operation. Command: " + command);
                    }
                } else {
                    LOG.error("Operation is not supported!");
                    throw new HttpException(ErrorResponseCode.UNSUPPORTED_OPERATION);
                }
            } catch (ClientErrorException e) {
                throw new WebApplicationException(e.getResponse().readEntity(String.class), e.getResponse().getStatus());
            } catch (WebApplicationException e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw e;
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }
        throw HttpException.internalError();
    }

}
