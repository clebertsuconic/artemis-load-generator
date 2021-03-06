/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.load.generator;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;

final class ProducerRunner {

   private ProducerRunner() {
   }

   public static void runJmsProducer(DestinationBench.BenchmarkConfiguration conf,
                                     Session session,
                                     Destination destination,
                                     final AtomicLong sentMessages,
                                     CloseableTickerEventListener eventListener) {
      MessageProducer producer = null;
      try (final CloseableTickerEventListener tickerEventListener = eventListener) {
         producer = session.createProducer(destination);
         producer.setDisableMessageTimestamp(true);
         switch (conf.delivery) {
            case Persistent:
               producer.setDeliveryMode(DeliveryMode.PERSISTENT);
               break;
            case NonPersistent:
               producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
               break;
            default:
               throw new AssertionError("unsupported case!");
         }
         final ByteBuffer clientContent = ByteBuffer.allocate(conf.messageBytes).order(ByteOrder.nativeOrder());
         final Ticker ticker;
         final MessageProducer localProducer = producer;
         final Ticker.ServiceAction serviceAction = (intendedStartTime, startServiceTime) -> {
            try {
               final BytesMessage message = session.createBytesMessage();
               BytesMessageUtil.encodeTimestamp(message, clientContent, intendedStartTime);
               localProducer.send(message);
               sentMessages.lazySet(sentMessages.get() + 1L);
            } catch (Throwable ex) {
               System.err.println(ex);
            }
         };
         if (conf.targetThoughput > 0) {
            ticker = Ticker.responseUnderLoadBenchmark(serviceAction, tickerEventListener, conf.targetThoughput, conf.iterations, conf.runs, conf.warmupIterations, conf.waitSecondsBetweenIterations, conf.isWaitRate);
         } else {
            ticker = Ticker.throughputBenchmark(serviceAction, tickerEventListener, conf.iterations, conf.runs, conf.warmupIterations, conf.waitSecondsBetweenIterations, conf.isWaitRate);
         }
         ticker.run();
      } catch (JMSException e) {
         throw new IllegalStateException(e);
      } finally {
         CloseableHelper.quietClose(producer);
      }
   }

}
