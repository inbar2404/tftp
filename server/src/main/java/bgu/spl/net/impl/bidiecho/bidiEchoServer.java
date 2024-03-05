package bgu.spl.net.impl.bidiecho;

import bgu.spl.net.srv.Server;

public class bidiEchoServer {

    public static void main(String[] args) {

        // you can use any server...
 //       Server.threadPerClient(
  //              7777, //port
   //             () -> new bidiEchoProtocol(), //protocol factory
   //             bidiEncoderDecoder::new //message encoder decoder factory
   //     ).serve();

    }
}
