import modules.dataFeeder.Feeder;
import modules.helper.options.OptionsHandler;
import modules.simulator.SimulatorHook;

public class SmartValidator {

    public static void main(String args[]){
//        OptionsHandler optionsHandler = OptionsHandler.getInstance();
        Feeder.getInstance().start();
	(new SimulatorHook()).main(null);
    }
}
