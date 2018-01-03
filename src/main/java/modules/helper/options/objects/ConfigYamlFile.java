package modules.helper.options.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigYamlFile
{
    @JsonProperty
    private DatabaseProps database;

    @JsonProperty
    private General general;

    @JsonProperty
    private Logger logger;

}
