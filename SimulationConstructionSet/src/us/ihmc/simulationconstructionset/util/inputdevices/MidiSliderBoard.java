package us.ihmc.simulationconstructionset.util.inputdevices;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

import us.ihmc.yoUtilities.dataStructure.YoVariableHolder;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

import us.ihmc.simulationconstructionset.ExitActionListener;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.inputdevices.MidiControl.ControlType;
import us.ihmc.simulationconstructionset.util.inputdevices.MidiControl.SliderType;

public class MidiSliderBoard implements ExitActionListener
{
   // There are problems with using both virtual and physical sliderboards at the same time when connected to the biped.
   private boolean alwaysShowVirtualSliderBoard = false;

   private enum Devices
   {
      VIRTUAL, MOTORIZED, GENERIC
   }

   public static final int CHECK_TIME = 10;

   // public int sliderOffset = 0;
   public int sliderBoardMax = 127;

   protected final Hashtable<Integer, MidiControl> controlsHashTable = new Hashtable<Integer, MidiControl>(40);
   protected final ArrayList<SliderListener> internalListeners = new ArrayList<SliderListener>();
   private final ArrayList<VariableChangedListener> variableChangedListeners = new ArrayList<VariableChangedListener>();

   private final ArrayList<SliderBoardControlAddedListener> controlAddedListeners = new ArrayList<SliderBoardControlAddedListener>();

   private Devices preferedDevice = Devices.VIRTUAL;
   private int preferdDeviceNumber = -1;

   private static final boolean DEBUG = false;

   private MidiDevice inDevice = null;
   private Receiver midiOut = null;
   private SliderBoardTransmitterInterface transmitter = null;
   private VirtualSliderBoardGui virtualSliderBoard;
   private VariableChangedListener listener;
   private YoVariableHolder holder;
   
   public MidiSliderBoard(SimulationConstructionSet scs)
   {
      this(scs, true);
   }

   public MidiSliderBoard(SimulationConstructionSet scs, boolean showVirtualSliderBoard)
   {
      try
      {
         MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

         // ArrayList<Integer> inDeviceNumbers = new ArrayList<Integer>();

         // findPotentialDeviceNumbers(infos, inDeviceNumbers, printInfo);

         init(infos);

         // if no devices are found

         if (scs != null)
         {
            this.holder = scs;
         }
         
         if (showVirtualSliderBoard && (preferedDevice.equals(Devices.VIRTUAL) || alwaysShowVirtualSliderBoard))
         {
            if ((scs == null) || (scs.getStandardSimulationGUI() != null))
            {
               System.out.println("Setting Up Virtual Slider Board");
               virtualSliderBoard = new VirtualSliderBoardGui(this);
            }

         }

         // if a motorized slider board is found
         if (preferedDevice.equals(Devices.MOTORIZED))
         {
            System.out.println("Setting Up Motorized Slider Board");

            // sliderOffset = 80;
            sliderBoardMax = 127;

            try
            {
               inDevice.getTransmitter().setReceiver(new BCF2000Receiver(controlsHashTable, internalListeners, variableChangedListeners, sliderBoardMax, this));

               transmitter = new BCF2000Transmitter(midiOut, sliderBoardMax);
            }
            catch (Exception e)
            {
               if (DEBUG)
                  System.err.println("Exception when trying to get MIDI transmitter 1: " + e);
            }

            final Object self = this;
            listener = new VariableChangedListener()
            {
               public void variableChanged(YoVariable v)
               {
                  synchronized (self)
                  {
                     for (MidiControl tmpControl : controlsHashTable.values())
                     {
                        if (tmpControl.var.equals(v))
                        {
                           double value = 0.0;
                           value = (tmpControl).var.getValueAsDouble();
                           yoVariableChanged((tmpControl).mapping, value);
                        }
                     }
                  }
               }
            };

            // Thread valueCheckerThread = new Thread(new ValueChecker());
            // valueCheckerThread.start();
         }

         // if a regular slider board is found
         else if (preferedDevice.equals(Devices.GENERIC))
         {
            // sliderOffset = -1;

            System.out.println("Setting Up Physical Slider Board");
            sliderBoardMax = 127;

            try
            {
               inDevice.getTransmitter().setReceiver(new UC33Receiver(controlsHashTable, internalListeners, variableChangedListeners, sliderBoardMax));
            }
            catch (Exception e)
            {
               if (DEBUG)
                  System.err.println("Exception when trying to get MIDI transmitter 1: " + e);
            }
         }

         addListener(new SliderListener()
         {
            public void valueChanged(MidiControl ctrl)
            {
               if (DEBUG)
                  System.out.println("EVL::valueChanged [" + ctrl.mapping + "] value to : " + ctrl.var.getValueAsDouble() + " YoVal:"
                        + ctrl.var.getValueAsDouble());
            }
         });
      }
      catch (Exception e)
      {
         e.printStackTrace();

      }

      if (scs != null)
      {
         scs.attachExitActionListener(this);
      }

   }

   public void closeAndDispose()
   {
      System.out.println("Closing And Disposing virtualSliderBoard");
      if (virtualSliderBoard != null)
         virtualSliderBoard.closeAndDispose();
      if (inDevice != null)
      {
         try
         {
            inDevice.close();
         }
         catch(Exception e)
         {
            System.err.println("Exception when trying to close inDevice in MidiSliderBoard.closeAndDispose()");
         }
      }
      if (midiOut != null)
      {
         try
         {
            midiOut.close();
         }
         catch(Exception e)
         {
            System.err.println("Exception when trying to close midiOut in MidiSliderBoard.closeAndDispose()");
         }
      }

      virtualSliderBoard = null;
      inDevice = null;
      midiOut = null;
   }

   public int init(MidiDevice.Info[] infos)
   {
      MidiDevice outDevice = null;
      if (DEBUG)
         System.out.println("EvolutionUC33E::init found " + infos.length + " MIDI device infos.");

      int rc = 0;

      String name = null;
      String description = null;
      MidiDevice current = null;
      for (int i = 0; i < infos.length; i++)
      {
         if (DEBUG)
         {
            System.out.println("  Device[" + i + "] " + infos[i].getName());
            System.out.println("          Vendor: " + infos[i].getVendor());
            System.out.println("         Version: " + infos[i].getVersion());
            System.out.println("     Description: " + infos[i].getDescription());
         }

         name = infos[i].getName();
         description = infos[i].getDescription();

         if ((name.indexOf("UC-33") >= 0) || (infos[i].getDescription().indexOf("UC-33") >= 0))
         {
            if (preferdDeviceNumber == -1)
            {
               preferdDeviceNumber = i;

               if (DEBUG)
                  System.out.println("Found Generic SliderBoard");
               preferedDevice = Devices.GENERIC;

               try
               {
                  inDevice = MidiSystem.getMidiDevice(infos[i]);
                  System.out.println("PHYSICAL " + inDevice.getDeviceInfo().getName() + " - " + inDevice.getDeviceInfo().getDescription());
               }
               catch (MidiUnavailableException e)
               {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }

               try
               {
                  if (DEBUG)
                  {
                     System.out.println("\nGot device with info" + inDevice.getDeviceInfo());
                     System.out.println("Max Receivers:" + inDevice.getMaxReceivers());
                     System.out.println("Max Transmitters:" + inDevice.getMaxTransmitters());
                  }

                  if (inDevice.isOpen())
                  {
                     if (DEBUG)
                        System.out.println("\nDevice started open. Closing Device\n");
                     inDevice.close();

                     if (inDevice.isOpen())
                     {
                        System.err.println("\nMIDI device started open. Attempted to close it, but could not. You may need to reboot to close the device.");
                     }
                  }

                  else
                  {
                     if (DEBUG)
                        System.out.println("Device started closed");
                  }

                  if (DEBUG)
                     System.out.println("\nOpening Device\n");

                  try
                  {
                     inDevice.open();
                  }
                  catch (Exception e)
                  {
                     if (DEBUG)
                        System.out.println("Exception while trying to open device: " + e);
                  }

               }

               catch (Exception e)
               {
                  System.out.println("Exception: " + e);
               }
            }
         }
         else if (name.contains("BCF2000") || description.contains("BCF2000"))
         {
            preferdDeviceNumber = i;
            preferedDevice = Devices.MOTORIZED;
            if (DEBUG)
               System.out.println("Found motorizedSliderBoard");

            try
            {
               current = MidiSystem.getMidiDevice(infos[i]);
            }
            catch (MidiUnavailableException e)
            {
               if (DEBUG)
               {
                  System.out.println("   - Unable to get a handle to this Midi Device.");
                  e.printStackTrace();
               }

               continue;
            }

            if ((outDevice == null) && (current.getMaxReceivers() != 0))
            {
               outDevice = current;

               if (!outDevice.isOpen())
               {
                  if (DEBUG)
                     System.out.println("   - Opening Output Device");

                  try
                  {
                     outDevice.open();
                  }
                  catch (MidiUnavailableException e)
                  {
                     outDevice = null;

                     if (DEBUG)
                     {
                        System.out.println("   - Unable to open device.");
                        e.printStackTrace();
                     }

                     continue;
                  }
               }

               if (DEBUG)
                  System.out.println("   - Device is Now open trying to obtain the receiver.");

               try
               {
                  midiOut = outDevice.getReceiver();
               }
               catch (MidiUnavailableException e)
               {
                  outDevice = null;
                  midiOut = null;

                  if (DEBUG)
                  {
                     System.out.println("   - Error getting the device's receiver.");
                     e.printStackTrace();
                  }

                  continue;
               }

               if (DEBUG)
                  System.out.println("   - Obtained a handle to the devices receiver.");
               rc += 2;
            }

            if ((inDevice == null) && (current.getMaxTransmitters() != 0))
            {
               inDevice = current;

               if (DEBUG)
               {
                  System.out.println("\nGot device with info" + inDevice.getDeviceInfo());
                  System.out.println("Max Receivers:" + inDevice.getMaxReceivers());
                  System.out.println("Max Transmitters:" + inDevice.getMaxTransmitters());
               }

               if (DEBUG)
                  System.out.println("   - Opening Input Device.");

               try
               {
                  inDevice.open();
               }
               catch (MidiUnavailableException e1)
               {
                  inDevice = null;

                  if (DEBUG)
                  {
                     System.out.println("   - Exception while trying to open device.");
                     e1.printStackTrace();
                  }

                  continue;
               }

               if (DEBUG)
                  System.out.println("   - Device is Now open trying to obtain the transmitter.");

               rc += 1;
            }
         }
      }

      return rc;
   }

   public void setButton(int channel, String name, YoVariableHolder holder)
   {
      setButton(channel, holder.getVariable(name));
   }

   public void setButton(int channel, YoVariable var)
   {
      int offset;
      if ((channel >= 17) && (channel <= 20))
         offset = -8;
      else
         offset = -16;
      setControl(channel + offset, var, 0, 1, 1, SliderType.BOOLEAN, ControlType.BUTTON);
   }

   public void setKnobButton(int channel, String name, YoVariableHolder holder)
   {
      setKnobButton(channel, holder.getVariable(name));
   }

   public void setKnobButton(int channel, YoVariable var)
   {
      setControl(channel - 48, var, 0, 1, 1, SliderType.BOOLEAN, ControlType.BUTTON);
   }

   public void setSlider(int channel, String name, YoVariableHolder holder, double min, double max)
   {
      setSlider(channel, name, holder, min, max, 1.0);
   }

   public void setSlider(int channel, String name, YoVariableHolder holder, double min, double max, double exponent)
   {
      if (!holder.hasUniqueVariable(name))
      {
         System.err.println("trying to add yovariable to slider, but it does not exist, or more than 1 exists: " + name);
      }

      setSlider(channel, holder.getVariable(name), min, max, exponent);
   }
   
   public void setSlider(int channel, String name, double min, double max, double exponent, double hires)
   { 
      setSlider(channel, name, holder, min, max, exponent, hires);
   }
   
   public void setSlider(int channel, String name, YoVariableHolder holder, double min, double max, double exponent, double hires)
   {  
      if (!holder.hasUniqueVariable(name))
      {
         System.err.println("trying to add yovariable to slider, but it does not exist, or more than 1 exists: " + name);
      }

      setSlider(channel, holder.getVariable(name), min, max, exponent, hires);
   }

   public void setSlider(int channel, YoVariable var, double min, double max)
   {
      setSlider(channel, var, min, max, 1.0);
   }

   public void setKnob(int channel, String name, YoVariableHolder holder, double min, double max)
   {
      setKnob(channel, name, holder, min, max, 1.0);
   }

   public void setKnob(int channel, String name, YoVariableHolder holder, double min, double max, double exponent)
   {
      if (!holder.hasUniqueVariable(name))
      {
         System.err.println("trying to add yovariable to knob, but it does not exist, or more than 1 exists: " + name);
      }
      
      setKnob(channel, holder.getVariable(name), min, max, exponent);
   }
   
   public void setKnob(int channel, String name, YoVariableHolder holder, double min, double max, double exponent, double hires)
   {
      if (!holder.hasUniqueVariable(name))
      {
         System.err.println("trying to add yovariable to knob, but it does not exist, or more than 1 exists: " + name);
      }
      
      setKnob(channel, holder.getVariable(name), min, max, exponent, hires);
   }

   public void setKnob(int channel, YoVariable var, double min, double max)
   {
      setKnob(channel, var, min, max, 1.0);
   }

   public void setSliderEnum(int channel, String name, YoVariableHolder holder)
   {
      setSliderEnum(channel, (EnumYoVariable<?>) holder.getVariable(name));
   }

   public void setSliderEnum(int channel, EnumYoVariable<?> var)
   {
      setControl(channel, var, 0.0, var.getEnumValues().length - 1, 1.0, SliderType.ENUM, ControlType.SLIDER);
   }

   public void setSliderBoolean(int channel, String name, YoVariableHolder holder)
   {
      setSliderBoolean(channel, holder.getVariable(name));
   }

   public void setSliderBoolean(int channel, YoVariable var)
   {
      setControl(channel, var, 0.0, 1.0, 1.0, SliderType.BOOLEAN, ControlType.SLIDER);
   }

   public void setSlider(int channel, YoVariable var, double min, double max, double exponent)
   {
      setControl(channel, var, min, max, exponent, SliderType.NUMBER, ControlType.SLIDER);
   }
   
   public void setSlider(int channel, YoVariable var, double min, double max, double exponent, double hires)
   {
      setControl(channel, var, min, max, exponent, hires, SliderType.NUMBER, ControlType.SLIDER);
   }

   public void setKnob(int channel, YoVariable var, double min, double max, double exponent)
   {
      setControl(channel - 80, var, min, max, exponent, SliderType.NUMBER, ControlType.KNOB);
   }
   
   public void setKnob(int channel, YoVariable var, double min, double max, double exponent, double hires)
   {
      setControl(channel - 80, var, min, max, exponent, hires, SliderType.NUMBER, ControlType.KNOB);
   }

   private void setControl(int channel, YoVariable var, double min, double max, double exponent, SliderType sliderType, ControlType controlType)
   {
      setControl(channel, var, min, max, exponent, (min+max)/2.0, sliderType, controlType);
   }
   
   private synchronized void setControl(int channel, YoVariable var, double min, double max, double exponent, double hires, SliderType sliderType, ControlType controlType)
   {
      if (var != null)
      {
         if (exponent <= 0.0)
         {
            System.err.println("Slider Board: Exponent must be positive. Setting it to 1.0");
            exponent = 1.0;
         }

         MidiControl ctrl = null;

         ctrl = new MidiControl(channel, var, max, min, exponent, hires);
         ctrl.sliderType = sliderType;
         ctrl.controlType = controlType;

         if (listener != null)
         {
            var.addVariableChangedListener(listener);
         }

         setControl(ctrl);
         setToInitialPosition(ctrl);

         for (SliderBoardControlAddedListener listener : controlAddedListeners)
         {
            listener.controlAdded(ctrl);
         }
      }
   }

   public synchronized void addListOfControlls(Collection<MidiControl> collection)
   {
      for (MidiControl control : collection)
      {
         setControl(control);
         setToInitialPosition(control);

         for (SliderBoardControlAddedListener listener : controlAddedListeners)
         {
            listener.controlAdded(control);
         }
      }
   }

   public void setRange(int channel, double min, double max)
   {
      setRange(channel, min, max, 1.0);
   }

   public void setRange(int channel, double min, double max, double exponent)
   {
      if (exponent <= 0.0)
      {
         System.err.println("Peavey PC1600X: Exponent must be positive. Setting it to 1.0");
         exponent = 1.0;
      }

      MidiControl control = controlsHashTable.get(channel);
      control.min = min;
      control.max = max;
      control.exponent = exponent;
   }

   private synchronized void setControl(MidiControl ctrl)
   {
      controlsHashTable.put(ctrl.mapping, ctrl);
   }

   public synchronized void clearControls()
   {
      for (MidiControl toBeRemoved : controlsHashTable.values())
      {
         for (SliderBoardControlAddedListener listener : controlAddedListeners)
         {
            listener.controlRemoved(toBeRemoved);
         }

      }

      controlsHashTable.clear();
   }

   protected void moveControl(MidiControl ctrl)
   {
      if (transmitter != null)
      {
         transmitter.moveControl(ctrl);
      }
   }

   protected void moveControl(MidiControl ctrl, int sliderValue)
   {
      if (transmitter != null)
      {
         transmitter.moveControl(ctrl, sliderValue);
      }
   }

   public void setToInitialPosition(MidiControl ctrl)
   {
      // ctrl.var.setValueFromDouble(ctrl.reset);

      for (SliderListener listener : internalListeners)
      {
         listener.valueChanged(ctrl);
      }

      moveControl(ctrl);
   }

   public void reset()
   {
      // ShortMessage sm = null;
      MidiControl ctrl = null;
      Enumeration<MidiControl> enctrls = controlsHashTable.elements();
      while (enctrls.hasMoreElements())
      {
         ctrl = enctrls.nextElement();
         ctrl.var.setValueFromDouble(ctrl.reset);

         for (SliderListener listener : internalListeners)
         {
            listener.valueChanged(ctrl);
         }

         moveControl(ctrl);
      }
   }

   public double getValue(int mapping)
   {
      MidiControl ctrl = controlsHashTable.get(mapping);
      if (ctrl != null)
         return ctrl.var.getValueAsDouble();

      return -1;
   }

   public void setValue(int mapping, double value) throws InvalidParameterException
   {
      MidiControl ctrl = controlsHashTable.get(mapping);
      if (ctrl == null)
         throw new InvalidParameterException("name does not map to a control");

      if (ctrl.currentVal == value)
         return;

      ctrl.currentVal = value;

      for (SliderListener listener : internalListeners)
      {
         listener.valueChanged(ctrl);
      }

      moveControl(ctrl);
   }

   public void yoVariableChanged(int mapping, double value) throws InvalidParameterException
   {
      MidiControl ctrl = controlsHashTable.get(mapping);
      if (ctrl == null)
         throw new InvalidParameterException("name does not map to a control");

      moveControl(ctrl);
   }

   public void addListener(SliderListener listener)
   {
      internalListeners.add(listener);
   }

   public void removeListener(SliderListener listener)
   {
      internalListeners.remove(listener);
   }

   public void addListener(SliderBoardControlAddedListener listener)
   {
      controlAddedListeners.add(listener);
   }

   public void removeListener(SliderBoardControlAddedListener listener)
   {
      controlAddedListeners.remove(listener);
   }

   public void attachVariableChangedListener(VariableChangedListener listener)
   {
      variableChangedListeners.add(listener);
   }

   public ArrayList<VariableChangedListener> getVariableChangedListeners()
   {
      return new ArrayList<VariableChangedListener>(variableChangedListeners);
   }

   public void exitActionPerformed()
   {
      this.closeAndDispose();
   }

   public interface SliderListener
   {
      public void valueChanged(MidiControl ctrl);
   }

   public void setVirtualSliderBoardFrameLocation(int x, int y)
   {
      virtualSliderBoard.setFrameLocation(x, y);
   }
}
