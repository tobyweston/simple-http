package bad.robot.http;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;

import static bad.robot.http.EmptyHeaders.emptyHeaders;
import static bad.robot.http.HeaderList.headers;
import static bad.robot.http.HeaderPair.header;
import static bad.robot.http.SequentialLinkIterator.sequentialLinkIterator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(JMock.class)
public class SequentialLinkIteratorTest {

    private final Mockery context = new JUnit4Mockery();
    private final HttpClient http = context.mock(HttpClient.class);

    private final HttpResponse initialResponse = new StringHttpResponse(200, "OK", "0", headers(header("Link", "<http://example.com/first>; rel=\"next\"")), "");
    private final HttpResponse secondResponse = new StringHttpResponse(201, "OK", "1", headers(header("Link", "<http://example.com/second>; rel=\"next\"")), "");
    private final HttpResponse finalResponse = new StringHttpResponse(202, "OK", "2", emptyHeaders(), "");

    private URL firstUrl;
    private URL secondUrl;

    @Before
    public void setUp() throws Exception {
        firstUrl = new URL("http://example.com/first");
        secondUrl = new URL("http://example.com/second");
    }

    @Test
    public void doesNotHaveNext() {
        assertThat(sequentialLinkIterator(finalResponse, http).hasNext(), is(false));
    }

    @Test
    public void shouldFollowLinks() throws MalformedURLException {
        final Sequence order = context.sequence("order");
        context.checking(new Expectations() {{
            oneOf(http).get(firstUrl); will(returnValue(secondResponse)); inSequence(order);
            oneOf(http).get(secondUrl); will(returnValue(finalResponse)); inSequence(order);
        }});
        String responses = "0";
        for (HttpResponse response : sequentialLinkIterator(initialResponse, http))
            responses += response.getContent().asString();
        assertThat(responses, is("012"));
    }

}
