package us.ihmc.aware.state;

import java.util.List;
import java.util.Map;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;

public class FiniteStateMachine<S extends Enum<S>, E extends Enum<E>>
{
   private final Map<S, FiniteStateMachineState<E>> states;

   /**
    * The list of possible transitions. This is equivalent to a state-transition function in FSM literature.
    */
   /*
    * NOTE: This should be a {@link java.util.Set}, but due to real-time constraints a {@link List} must be used
    * instead.
    */
   private final Map<Class<?>, List<FiniteStateMachineTransition<S, ? extends Enum<?>>>> transitions;

   private final Class<E> standardEventType;

   private final S initialState;

   /**
    * The present state.
    */
   private EnumYoVariable<S> state;

   /**
    * Whether or not the current state's {@link FiniteStateMachineState#onEntry()} needs to be called at the beginning of the next {@link #process()} call. This
    * is required because we don't want to call it immediately when the transition occurs. Rather, we want to wait until the next control cycle so the state's
    * {@link FiniteStateMachineState#onEntry()} and {@link FiniteStateMachineState#process()} methods are called in the same control loop.
    */
   // True so we don't forget to initialize the first state at startup
   private boolean needToCallOnEntry = true;

   /**
    * Use {@link FiniteStateMachineBuilder} instead.
    */
   FiniteStateMachine(Map<S, FiniteStateMachineState<E>> states,
         Map<Class<?>, List<FiniteStateMachineTransition<S, ? extends Enum<?>>>> transitions, S initialState, Class<S> enumType, Class<E> standardEventType,
         String yoVariableName, YoVariableRegistry registry)
   {
      this.states = states;
      this.transitions = transitions;
      this.initialState = initialState;
      this.standardEventType = standardEventType;
      this.state = new EnumYoVariable<>(yoVariableName, registry, enumType);
      this.state.set(initialState);
   }

   /**
    * Trigger the given event and follow the state transition, if it exists.
    * <p/>
    * If no transition is defined for the present state and given event then no action will be taken.
    *
    * @param event the triggered event.
    */
   public void trigger(E event)
   {
      trigger(standardEventType, event);
   }

   public <M extends Enum<M>> void trigger(Class<M> type, M event)
   {
      List<FiniteStateMachineTransition<S, ? extends Enum<?>>> transitionsOnE = transitions.get(type);
      for (int i = 0; i < transitionsOnE.size(); i++)
      {
         FiniteStateMachineTransition<S, ?> transition = transitionsOnE.get(i);

         // Check if this transition matches the source state and event.
         if (transition.getFrom() == getState() && event == transition.getEvent())
         {
            transition(transition.getFrom(), transition.getTo());
            break;
         }
      }
   }

   /**
    * Run the current state's {@link FiniteStateMachineState#process()} method and transition on any generated events.
    */
   public void process()
   {
      FiniteStateMachineState<E> instance = states.get(getState());

      // Call the delayed onEntry() function at the beginning of the process(), rather than at the end of the previous process().
      if (needToCallOnEntry)
      {
         instance.onEntry();
         needToCallOnEntry = false;
      }

      // Run the current state and see if it generates an event.
      E event = instance.process();

      if (event != null)
      {
         trigger(standardEventType, event);
      }
   }

   /**
    * {@see #state}
    */
   public S getState()
   {
      return state.getEnumValue();
   }

   /**
    * Forcefully set the current state.
    * <p/>
    * NOTE: Use this method with caution. It does not enforce reachability of the new state.
    *
    * @param state the new state
    */
   public void setState(S state)
   {
      this.state.set(state);
   }

   /**
    * Resets the state machine to the initial state, regardless of whether or not there is a transition to follow.
    */
   public void reset()
   {
      transition(getState(), initialState);
   }

   private FiniteStateMachineState<?> getInstanceForEnum(S state)
   {
      if (!states.containsKey(state))
      {
         throw new IllegalArgumentException("State " + state + " is not registered");
      }

      return states.get(state);
   }

   private void transition(S from, S to)
   {
      FiniteStateMachineState<?> fromInstance = getInstanceForEnum(from);

      // It does, so transition to the next state.
      fromInstance.onExit();
      setState(to);

      needToCallOnEntry = true;
   }
}
