package us.ihmc.aware.state;

import java.util.List;
import java.util.Map;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;

public class StateMachine<S extends Enum<S>, E extends Enum<E>>
{
   private final Map<S, StateMachineState<E>> states;

   /**
    * The list of possible transitions. This is equivalent to a state-transition function in FSM literature.
    */
   /*
    * NOTE: This should be a {@link java.util.Set}, but due to real-time constraints a {@link List} must be used
    * instead.
    */
   private final List<StateMachineTransition<S, E>> transitions;

   private final S initialState;

   /**
    * The present state.
    */
   private EnumYoVariable<S> state;

   /**
    * Whether or not the current state's {@link StateMachineState#onEntry()} needs to be called at the beginning of the next {@link #process()} call. This is
    * required because we don't want to call it immediately when the transition occurs. Rather, we want to wait until the next control cycle so the state's
    * {@link StateMachineState#onEntry()} and {@link StateMachineState#process()} methods are called in the same control loop.
    */
   // True so we don't forget to initialize the first state at startup
   private boolean needToCallOnEntry = true;

   /**
    * Use {@link StateMachineBuilder} instead.
    */
   StateMachine(Map<S, StateMachineState<E>> states, List<StateMachineTransition<S, E>> transitions, S initialState, Class<S> enumType, String yoVariableName,
         YoVariableRegistry registry)
   {
      this.states = states;
      this.transitions = transitions;
      this.initialState = initialState;
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
      for (int i = 0; i < transitions.size(); i++)
      {
         StateMachineTransition<S, E> transition = transitions.get(i);

         // Check if this transition matches the source state and event.
         if (transition.getFrom() == getState() && event == transition.getEvent())
         {
            transition(transition.getFrom(), transition.getTo());
            break;
         }
      }
   }

   /**
    * Run the current state's {@link StateMachineState#process()} method and transition on any generated events.
    */
   public void process()
   {
      StateMachineState<E> instance = states.get(getState());

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
         trigger(event);
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
    * @return the current state, as an instance of {@link StateMachineState}
    */
   public StateMachineState<E> getStateInstance()
   {
      return getInstanceForEnum(getState());
   }

   /**
    * Resets the state machine to the initial state, regardless of whether or not there is a transition to follow.
    */
   public void reset()
   {
      transition(getState(), initialState);
   }

   private StateMachineState<E> getInstanceForEnum(S state)
   {
      if (!states.containsKey(state))
      {
         throw new IllegalArgumentException("State " + state + " is not registered");
      }

      return states.get(state);
   }

   private void transition(S from, S to)
   {
      StateMachineState<E> fromInstance = getInstanceForEnum(from);

      // It does, so transition to the next state.
      fromInstance.onExit();
      setState(to);

      needToCallOnEntry = true;
   }
}
