package models;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import protobuf.WeatherstationV1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * Class Measurement
 */
public class Measurement extends BaseModel implements Hydrate {

    /**
     * Het station waarvan deze gegevens zijn
     * <p>Example:
     * {@code
     * <STN>123456</STN>
     * }
     * </p>
     */
    public int station;

    /**
     * Datum van versturen van deze gegevens, formaat: yyyy-mm-dd
     * <p>Example:
     * {@code
     * <DATE>2009-09-13</DATE>
     * }
     * </p>
     */
    public LocalDateTime dateTime;

    /**
     * Gebeurtenissen op deze dag, cummulatief, binair uitgedrukt.
     * Opeenvolgend, van meest- naar minst significant:
     * Vriezen, geeft aan of het gevroren heeft
     * Regen, geeft aan of het geregend heeft.
     * Sneeuw, geeft aan of het gesneeuwd heeft.
     * Hagel, geeft aan of het gehageld heeft.
     * Onweer, geeft aan of er onweer is geweest.
     * Tornado/windhoos, geeft aan of er een tornado of windhoos geweest is.
     * <p>Example:
     * {@code
     * <FRSHTT>010101</FRSHTT>
     * }
     * </p>
     */
    public int events;

    enum Fields {
        /**
         * Temperatuur in graden Celsius, geldige waardes van -9999.9 t/m 9999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <TEMP>-60.1</TEMP>
         * }
         * </p>
         */
        TEMP,
        /**
         * Gevallen sneeuw in centimeters, geldige waardes van -9999.9 t/m 9999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <SNDP>11.1</SNDP>
         * }
         * </p>
         */
        SNDP,
        /**
         * Neerslag in centimeters, geldige waardes van 0.00 t/m 999.99 met 2 decimalen
         * <p>Example:
         * {@code
         * <PRCP>11.28</PRCP>
         * }
         * </p>
         */
        PRCP,
        /**
         * Windsnelheid in kilometers per uur, geldige waardes van 0.0 t/m 999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <WDSP>10.8</WDSP>
         * }
         * </p>
         */
        WDSP,
        /**
         * Zichtbaarheid in kilometers, geldige waardes van 0.0 t/m 999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <VISIB>123.7</VISIB>
         * }
         * </p>
         */
        VISIB,
        /**
         * Luchtdruk op zeeniveau in millibar, geldige waardes van 0.0 t/m 9999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <SLP>1007.6</SLP>
         * }
         * </p>
         */
        SLP,
        /**
         * Luchtdruk op stationsniveau in millibar, geldige waardes van 0.0 t/m 9999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <STP>1034.5</STP>
         * }
         * </p>
         */
        STP,
        /**
         * Dauwpunt in graden Celsius, geldige waardes van -9999.9 t/m 9999.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <DEWP>-58.1</DEWP>
         * }
         * </p>
         */
        DEWP,
        /**
         * Bewolking in procenten, geldige waardes van 0.0 t/m 99.9 met 1 decimaal
         * <p>Example:
         * {@code
         * <CLDC>87.4</CLDC>
         * }
         * </p>
         */
        CLDC,
        /**
         * Windrichting in graden, geldige waardes van 0 t/m 359 alleen gehele getallen
         * <p>Example:
         * {@code
         * <WNDDIR>342</WNDDIR>
         * }
         * </p>
         */
        WNDDIR
    }

    HashMap<Fields, Float> fields;

    public Measurement() {
        fields = new HashMap<>();
    }

    public Measurement(Node node) {
        this();
        load(node);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d %s:\n", station, dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        for (HashMap.Entry<Fields, Float> entry : fields.entrySet()) {
            sb.append("\t" + entry.getKey().toString() + " = " + entry.getValue() + "\n");
        }
        return sb.toString();
    }

    @Override
    public void load(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            station = Integer.parseInt(getTagValue("STN", element));
            String datetime = getTagValue("DATE", element) + " " + getTagValue("TIME", element);
            this.dateTime = LocalDateTime.from(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").parse(datetime));
            String events = getTagValue("FRSHTT", element);
            this.events = (events == null) ? 0 : Integer.parseInt(events);
            for (Fields field : Fields.values()) {
                String value = getTagValue(field.toString(), element);
                fields.put(field, (value != null) ? Float.parseFloat(value) : null);
            }
        }
    }

    public WeatherstationV1.Measurement toProtobuf() {
        return WeatherstationV1.Measurement.newBuilder()
                .setStation(station)
                .setDatetime((int) dateTime.atZone(ZoneId.systemDefault()).toEpochSecond())
                .setDewpoint(fields.get(Fields.DEWP))
                .setFallenSnow(fields.get(Fields.SNDP))
                .setOvercast(fields.get(Fields.CLDC))
                .setPrecipitation(fields.get(Fields.PRCP))
                .setSeaAirPressure(fields.get(Fields.SLP))
                .setStationAirPressure(fields.get(Fields.STP))
                .setTemperature(fields.get(Fields.TEMP))
                .setVisibility(fields.get(Fields.VISIB))
                .setFreeze((events & 1) != 0) // check 0th bit
                .setRain((events & 1 << 1) != 0) // check 1th bit
                .setSnow((events & 1 << 2) != 0) // check 2th bit
                .setHail((events & 1 << 3) != 0) // check 3th bit
                .setStorm((events & 1 << 4) != 0) // check 4th bit
                .setTornado((events & 1 << 5) != 0) // check 5th bit
                .build();
    }

    public String getFilename(String path) {
        path = (path.endsWith(File.separator)) ? path : path + File.separatorChar;
        return String.format(path + "%s" + File.separatorChar + "%d" + File.separatorChar + "%d.dat",
                dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                station,
                dateTime.getHour());
    }

    public void saveToFile(String path) throws IOException {
        File file = new File(getFilename(path));
        File directory = file.getParentFile();
        // check if directory exists where we want to store the measurement
        if (!directory.exists()) {
            directory.mkdirs();
        }
        toProtobuf().writeTo(new FileOutputStream(file));
    }
}
