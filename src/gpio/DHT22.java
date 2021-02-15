package gpio;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class DHT22 {
    private static final int    MAXTIMINGS  = 85;
    private final int[]         dht22_dat   = { 0, 0, 0, 0, 0 };

    public DHT22() {

        // setup wiringPi
        if (Gpio.wiringPiSetup() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            return;
        }

        GpioUtil.export(4, GpioUtil.DIRECTION_OUT);
    }

    public void getTemperature(final int pin) {
        int laststate = Gpio.HIGH;
        int j = 0;
        dht22_dat[0] = dht22_dat[1] = dht22_dat[2] = dht22_dat[3] = dht22_dat[4] = 0;

        Gpio.pinMode(pin, Gpio.OUTPUT);
        Gpio.digitalWrite(pin, Gpio.LOW);
        Gpio.delay(18);

        Gpio.digitalWrite(pin, Gpio.HIGH);
        Gpio.pinMode(pin, Gpio.INPUT);

        for (int i = 0; i < MAXTIMINGS; i++) {
            int counter = 0;
            while (Gpio.digitalRead(pin) == laststate) {
                counter++;
                Gpio.delayMicroseconds(1);
                if (counter == 255) {
                    break;
                }
            }

            laststate = Gpio.digitalRead(pin);

            if (counter == 255) {
                break;
            }

            /* ignore first 3 transitions */
            if (i >= 4 && i % 2 == 0) {
                /* shove each bit into the storage bytes */
                dht22_dat[j / 8] <<= 1;
                if (counter > 16) {
                    dht22_dat[j / 8] |= 1;
                }
                j++;
            }
        }
        // check we read 40 bits (8bit x 5 ) + verify checksum in the last
        // byte
        if (j >= 40 && checkParity()) {
            float h = (float) ((dht22_dat[0] << 8) + dht22_dat[1]) / 10;
            if (h > 100) {
                h = dht22_dat[0]; // for DHT11
            }
            float c = (float) (((dht22_dat[2] & 0x7F) << 8) + dht22_dat[3]) / 10;
            if (c > 125) {
                c = dht22_dat[2]; // for DHT11
            }
            if ((dht22_dat[2] & 0x80) != 0) {
                c = -c;
            }
            final float f = c * 1.8f + 32;
            System.out.println("Humidity = " + h + " Temperature = " + c + "(" + f + "f)");
        } else {
            System.out.println("Data not good, skip");
        }

    }

    private boolean checkParity() {
        return dht22_dat[4] == (dht22_dat[0] + dht22_dat[1] + dht22_dat[2] + dht22_dat[3] & 0xFF);
    }

    public static void main(final String ars[]) throws Exception {

        final DHT22 dht = new DHT22();

        for (int i = 0; i < 10; i++) {
            Thread.sleep(2000);
            dht.getTemperature(21);
        }

        System.out.println("Done!!");
    }
}