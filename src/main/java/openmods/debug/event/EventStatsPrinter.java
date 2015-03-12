package openmods.debug.event;

import java.io.PrintWriter;

import openmods.debug.event.EventCollector.EventVisitor;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.IEventListener;

public class EventStatsPrinter implements EventVisitor {

    private final PrintWriter writer;

    public EventStatsPrinter(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public void startProbe(String bus) {
        writer.format("Printing events from probe %s\n", bus);
    }

    @Override
    public void visitProbeEvents(Class<? extends Event> cls, int count) {
        writer.format("\tEvent class: %s, use count: %d\n", cls, count);
    }

    @Override
    public void endProbe() {}

    @Override
    public void startEventClass(Class<? extends Event> cls) {
        writer.format("Printing listeners from %s\n", cls);
    }

    @Override
    public void startEventBus(int busId) {
        writer.format("\tBus: %d\n", busId);
    }

    @Override
    public void visitUnknownListener(IEventListener listener) {
        writer.format("\t\tUnknown listener: %s (%s)\n", listener, listener.getClass());
    }

    @Override
    public void visitPriorityMarker(EventPriority listener) {
        writer.format("\t\tPriority change: %s\n", listener.name());
    }

    @Override
    public void visitAsmListener(Object target, EventPriority priority) {
        writer.format("\t\tASM listener: %s, priority: %s\n", target, priority);
    }

    @Override
    public void endEventBus() {}

    @Override
    public void endEventClass() {}

}
