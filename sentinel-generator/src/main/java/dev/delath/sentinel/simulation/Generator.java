package dev.delath.sentinel.simulation;

import dev.delath.sentinel.domain.Transaction;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Generator {

  public Transaction next() {
    return new Transaction(
        // WIP
        );
  }
}
