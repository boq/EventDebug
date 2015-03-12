package openmods.debug.event;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

import cpw.mods.fml.common.eventhandler.*;

public class EventCollector {
    private final Map<String, EventProbe> probes = Maps.newHashMap();

    private static final FieldAccess<Object> HANDLER_ACCESS = FieldAccess.create(ASMEventHandler.class, "handler");

    public class EventProbe {
        private final Multiset<Class<? extends Event>> eventClasses = HashMultiset.create();

        @SubscribeEvent
        public void onEvent(Event event) {
            eventClasses.add(event.getClass());
        }

        public void visit(EventVisitor visitor) {
            for (Multiset.Entry<Class<? extends Event>> e : eventClasses.entrySet())
                visitor.visitProbeEvents(e.getElement(), e.getCount());
        }

        public void addClasses(Set<Class<? extends Event>> events) {
            events.addAll(eventClasses.elementSet());
        }
    }

    public Object createProbe(String bus) {
        Preconditions.checkState(!probes.containsKey(bus), "Duplicate probe: " + bus);
        EventProbe probe = new EventProbe();
        probes.put(bus, probe);
        return probe;
    }

    public interface EventVisitor {
        public void startProbe(String bus);

        public void visitProbeEvents(Class<? extends Event> cls, int count);

        public void endProbe();

        public void startEventClass(Class<? extends Event> cls);

        public void startEventBus(int busId);

        public void visitUnknownListener(IEventListener listener);

        public void visitPriorityMarker(EventPriority listener);

        public void visitAsmListener(Object target, EventPriority priority);

        public void endEventBus();

        public void endEventClass();
    }

    private static void visitHandlers(EventVisitor visitor, Class<? extends Event> cls) {
        try {
            Event evt = cls.newInstance();
            ListenerList listeners = evt.getListenerList();

            try {
                int busId = 0;
                while (true) {
                    visitor.startEventBus(busId);
                    for (IEventListener listener : listeners.getListeners(busId)) {
                        if (listener instanceof ASMEventHandler) {
                            try {
                                final ASMEventHandler wrapper = (ASMEventHandler)listener;
                                Object target = HANDLER_ACCESS.get(wrapper);
                                visitor.visitAsmListener(target, wrapper.getPriority());
                            } catch (Throwable e) {
                                Log.log(Level.DEBUG, e, "Exception while getting field");
                            }
                        } else if (listener instanceof EventPriority) {
                            visitor.visitPriorityMarker((EventPriority)listener);
                        } else {
                            visitor.visitUnknownListener(listener);
                        }
                    }
                    visitor.endEventBus();
                    busId++;
                }
            } catch (ArrayIndexOutOfBoundsException terribleLoopExitCondition) {}
        } catch (Throwable t) {
            Log.warn(t, "Failed to instantiate class %s", cls);
        }
    }

    public void visitStats(EventVisitor visitor) {
        Set<Class<? extends Event>> allEvents = Sets.newHashSet();
        for (Map.Entry<String, EventProbe> e : probes.entrySet()) {
            visitor.startProbe(e.getKey());
            final EventProbe probe = e.getValue();
            probe.visit(visitor);
            probe.addClasses(allEvents);
            visitor.endProbe();
        }

        for (Class<? extends Event> cls : allEvents) {
            visitor.startEventClass(cls);
            visitHandlers(visitor, cls);
            visitor.endEventClass();
        }
    }

}
