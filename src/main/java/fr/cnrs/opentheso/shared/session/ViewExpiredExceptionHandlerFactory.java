
package fr.cnrs.opentheso.shared.session;

/**
 *
 * @author miled.rousset
 */
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

public class ViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {

    private ExceptionHandlerFactory factory;

    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory factory) {
        this.factory = factory;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        ExceptionHandler handler = factory.getExceptionHandler();
        handler = new ViewExpiredExceptionHandler(handler);
        return handler;
    }

}
