package uk.tethys.survival.message.template;

import java.util.function.Supplier;

public abstract class BasicMessageFactory {

    public abstract Supplier<String> getSupplier();

}
