package api.reporting;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * A REST Assured filter that records every HTTP exchange the client
 * makes — method, URI, status, elapsed time and a body excerpt — so
 * the report listener can show exactly what each test sent and got
 * back, instead of a bare pass/fail. Exchanges are kept per thread and
 * drained by the listener when the test finishes; a call that never
 * got a response is recorded with status -1 and the failure text.
 */
public final class ExchangeCapture implements Filter {

    /** One captured request/response pair. */
    public record Exchange(String method, String uri, int status, long elapsedMillis,
                           String contentType, String bodyExcerpt, String error) {

        /** One-line rendering for the report, e.g. "POST /cart -> 200 (14 ms)". */
        public String summary() {
            String target = uri;
            if (error != null) {
                return method + " " + target + " -> FAILED (" + elapsedMillis + " ms): " + error;
            }
            return method + " " + target + " -> " + status + " (" + elapsedMillis + " ms)";
        }
    }

    /** The single instance the client attaches to its requests. */
    public static final ExchangeCapture INSTANCE = new ExchangeCapture();

    private static final int MAX_BODY_CHARS = 1500;
    private static final ThreadLocal<List<Exchange>> CURRENT = ThreadLocal.withInitial(ArrayList::new);

    private ExchangeCapture() {
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec, FilterContext ctx) {
        long started = System.nanoTime();
        try {
            Response response = ctx.next(requestSpec, responseSpec);
            String body = "";
            try {
                body = response.getBody().asString();
            } catch (RuntimeException ignored) {
                // A body that cannot be read is not worth failing a call over.
            }
            if (body != null && body.length() > MAX_BODY_CHARS) {
                body = body.substring(0, MAX_BODY_CHARS) + "\n... [truncated]";
            }
            CURRENT.get().add(new Exchange(requestSpec.getMethod(), requestSpec.getURI(),
                    response.getStatusCode(), elapsedMillis(started),
                    response.getContentType(), body == null ? "" : body, null));
            return response;
        } catch (Throwable failure) {
            // REST Assured sneaky-throws checked IOExceptions (connection
            // resets, refusals) past its signatures; those exchanges are
            // exactly the ones a report must not lose.
            CURRENT.get().add(new Exchange(requestSpec.getMethod(), requestSpec.getURI(),
                    -1, elapsedMillis(started), "", "", failure.toString()));
            throw ExchangeCapture.<RuntimeException>sneakyThrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable failure) throws T {
        throw (T) failure;
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    /**
     * Returns the exchanges captured on this thread since the last
     * drain and clears the store, so each test reports only its own
     * traffic.
     *
     * @return the captured exchanges, in call order
     */
    public static List<Exchange> drain() {
        List<Exchange> exchanges = CURRENT.get();
        CURRENT.remove();
        return exchanges;
    }
}
