import modules.dataFeeder.Feeder;
import modules.helper.options.OptionsHandler;

public class SmartValidator {

    public static void main(String args[]){
//        OptionsHandler optionsHandler = OptionsHandler.getInstance();
        Feeder.getInstance().start();
    }
}
