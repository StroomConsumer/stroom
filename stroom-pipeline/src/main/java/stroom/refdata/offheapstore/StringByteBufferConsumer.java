/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.refdata.offheapstore;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.refdata.offheapstore.serdes.StringValueSerde;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import javax.inject.Inject;
import java.nio.ByteBuffer;

public class StringByteBufferConsumer extends AbstractByteBufferConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringByteBufferConsumer.class);

    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(StringByteBufferConsumer.class);

    static final Location NULL_LOCATION = new NullLocation();
    private final StringValueSerde stringValueSerde;

    StringByteBufferConsumer(final Receiver receiver, final StringValueSerde stringValueSerde) {
        super(receiver);
        this.stringValueSerde = stringValueSerde;
    }

    @Override
    public void consumeBytes(final Receiver receiver, final ByteBuffer byteBuffer) {
        LOGGER.trace("consumeBytes()");

        // we should only be consuming string type values
        final String str = StringValueSerde.decodeString(byteBuffer);
        byteBuffer.flip();

        LAMBDA_LOGGER.trace(() -> LambdaLogger.buildMessage("str {}, byteBuffer {}",
                str, ByteArrayUtils.byteBufferInfo(byteBuffer)));

        try {
            receiver.characters(str, NULL_LOCATION, ReceiverOptions.WHOLE_TEXT_NODE);
        } catch (XPathException e) {
            throw new RuntimeException(LambdaLogger.buildMessage("Error passing string {} to receiver", str), e);
        }
    }

    public static class Factory implements AbstractByteBufferConsumer.Factory {

        private final StringValueSerde stringValueSerde;

        @Inject
        public Factory(final StringValueSerde stringValueSerde) {
            this.stringValueSerde = stringValueSerde;
        }

        @Override
        public RefDataValueByteBufferConsumer create(final Receiver receiver,
                                                 final PipelineConfiguration pipelineConfiguration) {
            return new StringByteBufferConsumer(receiver, stringValueSerde);
        }
    }
}
