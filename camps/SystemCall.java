/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package camps;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;

/**
 *
 * @author toepel
 */
public class SystemCall {
    // result field
    private StringBuffer result = new StringBuffer();

    /*
     * Method to make a system call on the server where the bean is deployed.
     * simply provide a String containing the command.
     */
    public String call(String systemcall) {
        Runtime r = Runtime.getRuntime();
        try {
            /*
             * Here we are executing the system call
             */
            Process p = r.exec(systemcall);
            InputStream in = p.getInputStream();
            BufferedInputStream buf = new BufferedInputStream(in);
            InputStreamReader inread = new InputStreamReader(buf);
            BufferedReader bufferedreader = new BufferedReader(inread);

            // Read the output linewise
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                System.out.println(line);
                result.append(line);
                result.append("\n");
            }

            try {
                if (p.waitFor() != 0) {
                    Logger.getLogger(SystemCall.class.getName()).log(Level.SEVERE, "process exit value = {0}", p.exitValue());
                }
            } catch (InterruptedException e) {
                Logger.getLogger(SystemCall.class.getName()).log(Level.SEVERE,null,e);
                throw new WebApplicationException(500);
            } finally {
                // Close the InputStream
                bufferedreader.close();
                inread.close();
                buf.close();
                in.close();
            }
        } catch (IOException e) {
            Logger.getLogger(SystemCall.class.getName()).log(Level.SEVERE,null,e);
            throw new WebApplicationException(500);
        }
        return result.toString();
    }

}
