/*
Copyright 2001-2013 Aurea, Inc. All Rights Reserved.

Sample Application

Writing a Basic JMS Application using Publish and Subscribe

This sample publishes and subscribes to a specified topic.
Text you enter is published and then received by all subscribers.

Usage:
  java Chat -b <broker:port> -u <username> -p <password>
      -b broker:port points to your message broker
                     Default: localhost:2506
      -u username    must be unique (but is not checked)
      -p password    password for user (not checked)

Suggested demonstration:
  - In separate console windows with the environment set,
    start instances of the application
    under unique user names.
    For example:
       java Chat -b localhost:2506 -u SALES
       java Chat -b localhost:2506 -u MARKETING
  - Enter text and then press Enter to publish the message.
  - See messages appear under the various user names as you
    enter messages in each console window.
  - Stop a session by pressing CTRL+C in its console window.

*/


public class Chat
    implements javax.jms.MessageListener
{
    private static final String APP_TOPIC = "jms.samples.chat";
    private static final String DEFAULT_BROKER_NAME = "localhost:2506";
    private static final String DEFAULT_PASSWORD = "Adminsitrator";

    private javax.jms.Connection connect = null;
    private javax.jms.Session pubSession = null;
    private javax.jms.Session subSession = null;
    private javax.jms.MessageProducer publisher = null;

    /** Create JMS client for publishing and subscribing to messages. */
    private void chatter( String broker, String username, String password)
    {
        // Create a connection.
        try
        {
            javax.jms.ConnectionFactory factory;
            factory = (new progress.message.jclient.ConnectionFactory (broker));
            connect = factory.createConnection (username, password);
            pubSession = connect.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
            subSession = connect.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
        }
        catch (javax.jms.JMSException jmse)
        {
            System.err.println("error: Cannot connect to Broker - " + broker);
            jmse.printStackTrace();
            System.exit(1);
        }

        // Create Publisher and Subscriber to 'chat' topics
        try
        {
            javax.jms.Topic topic = pubSession.createTopic (APP_TOPIC);
            javax.jms.MessageConsumer subscriber = subSession.createConsumer(topic);
            subscriber.setMessageListener(this);
            publisher = pubSession.createProducer(topic);
            // Now that setup is complete, start the Connection
            connect.start();
        }
        catch (javax.jms.JMSException jmse)
        {
            jmse.printStackTrace();
        }

        try
        {
            // Read all standard input and send it as a message.
            java.io.BufferedReader stdin =
                new java.io.BufferedReader( new java.io.InputStreamReader( System.in ) );
            System.out.println ("\nEnter text messages to clients that subscribe to the " + APP_TOPIC + " topic." +
                                "\nPress Enter to publish each message.\n");
            while ( true )
            {
                String s = stdin.readLine();

                if ( s == null )
                    exit();
                else if ( s.length() > 0 )
                {
                    javax.jms.TextMessage msg = pubSession.createTextMessage();
                    msg.setText( username + ": " + s );
                    publisher.send( msg );
                }
            }
        }
        catch ( java.io.IOException ioe )
        {
            ioe.printStackTrace();
        }
        catch ( javax.jms.JMSException jmse )
        {
            jmse.printStackTrace();
        }
    }

    /**
     * Handle the message
     * (as specified in the javax.jms.MessageListener interface).
     */
    public void onMessage( javax.jms.Message aMessage)
    {
        try
        {
            // Cast the message as a text message.
            javax.jms.TextMessage textMessage = (javax.jms.TextMessage) aMessage;

            // This handler reads a single String from the
            // message and prints it to the standard output.
            try
            {
                String string = textMessage.getText();
                System.out.println( string );
            }
            catch (javax.jms.JMSException jmse)
            {
                jmse.printStackTrace();
            }
        }
        catch (java.lang.RuntimeException rte)
        {
            rte.printStackTrace();
        }
    }

    /** Cleanup resources and then exit. */
    private void exit()
    {
        try
        {
            connect.close();
        }
        catch (javax.jms.JMSException jmse)
        {
            jmse.printStackTrace();
        }

        System.exit(0);
    }

    //
    // NOTE: the remainder of this sample deals with reading arguments
    // and does not utilize any JMS classes or code.
    //

    /** Main program entry point. */
    public static void main(String argv[]) {

        // Is there anything to do?
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }

        // Values to be read from parameters
        String broker    = DEFAULT_BROKER_NAME;
        String username  = "Administrator";
        String password  = DEFAULT_PASSWORD;

        // Check parameters
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];


            if (arg.equals("-b")) {
                if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                    System.err.println("error: missing broker name:port");
                    System.exit(1);
                }
                broker = argv[++i];
                continue;
            }

            if (arg.equals("-u")) {
                if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                    System.err.println("error: missing user name");
                    System.exit(1);
                }
                username = argv[++i];
                continue;
            }

            if (arg.equals("-p")) {
                if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                    System.err.println("error: missing password");
                    System.exit(1);
                }
                password = argv[++i];
                continue;
            }

            if (arg.equals("-h")) {
                printUsage();
                System.exit(1);
            }

            // Invalid argument
            System.err.println ("error: unexpected argument: "+arg);
            printUsage();
            System.exit(1);
        }

        // Check values read in.
        if (username == null) {
            System.err.println ("error: user name must be supplied");
            printUsage();
            System.exit(1);
        }

        // Start the JMS client for the "chat".
        Chat chat = new Chat();
        chat.chatter (broker, username, password);

    }

    /** Prints the usage. */
    private static void printUsage() {

        StringBuffer use = new StringBuffer();
        use.append("usage: java Chat (options) ...\n\n");
        use.append("options:\n");
        use.append("  -b name:port Specify name:port of broker.\n");
        use.append("               Default broker: "+DEFAULT_BROKER_NAME+"\n");
        use.append("  -u name      Specify unique user name. (Required)\n");
        use.append("  -p password  Specify password for user.\n");
        use.append("               Default password: "+DEFAULT_PASSWORD+"\n");
        use.append("  -h           This help screen.\n");
        System.err.println (use);
    }

}

