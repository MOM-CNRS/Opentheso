package fr.cnrs.opentheso.v2.test.support;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.primefaces.PrimeFaces;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public final class PrimeFacesTestSupport {

    private PrimeFacesTestSupport() {
    }

    public static PrimeFacesContext open() {
        PrimeFaces current = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(current.ajax()).thenReturn(ajax);
        MockedStatic<PrimeFaces> staticMock = Mockito.mockStatic(PrimeFaces.class);
        staticMock.when(PrimeFaces::current).thenReturn(current);
        return new PrimeFacesContext(staticMock, current, ajax);
    }

    public record PrimeFacesContext(
            MockedStatic<PrimeFaces> staticMock,
            PrimeFaces current,
            PrimeFaces.Ajax ajax
    ) implements AutoCloseable {

        @Override
        public void close() {
            staticMock.close();
        }
    }
}
