package dev.delath.sentinel.simulation;

import dev.delath.sentinel.domain.Transaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Publisher {

  private static final Logger LOG = Logger.getLogger(Publisher.class);

  @Inject Generator generator;

  @Inject
  @Channel("generated-transactions")
  Emitter<Transaction> eventEmitter;

  @Scheduled(every = "500ms")
  public void publish() {
    var transaction = generator.next();

    eventEmitter.send(transaction);

    LOG.infof("Sentinel: Emitted transaction %s", transaction.transactionId());
  }
}
