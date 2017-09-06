## A Proof of a Product  
Using Damgard-Jurik cryptosystem for message exchange, to provide a zero knowledge proof that an agent correctly calculated a product of two numbers 


### Run and test
`sbt run` runs basic exchange
```
> run
[info] Running actors.Main 
[INFO] [09/07/2017 00:42:10.868] [dj-akka.actor.default-dispatcher-3] [akka://dj/user/bob] encrypt 5
[INFO] [09/07/2017 00:42:10.868] [dj-akka.actor.default-dispatcher-4] [akka://dj/user/alice] encrypt 4
[INFO] [09/07/2017 00:42:10.914] [dj-akka.actor.default-dispatcher-4] [akka://dj/user/alice] Invitation received; sending cipherText BigIntegerCiphertext [cipher=2097190805125431317834021486638388978934851656533306602485144438619570874427428669949707307763545753558983801001393988788344291587092998865472909915693863]
[INFO] [09/07/2017 00:42:10.914] [dj-akka.actor.default-dispatcher-3] [akka://dj/user/bob] Invitation received; sending cipherText BigIntegerCiphertext [cipher=1464075767704111868366871632723114493137783296142772398310296941236906812293654972038256230965827496013137957695695779908427802017504352982801019690835502]
[INFO] [09/07/2017 00:42:10.918] [dj-akka.actor.default-dispatcher-5] [akka://dj/user/carroll] Proceeding with Multiply BigIntegerPlainText [x=5]*BigIntegerPlainText [x=4]
[INFO] [09/07/2017 00:42:10.919] [dj-akka.actor.default-dispatcher-5] [akka://dj/user/carroll] product BigIntegerPlainText [x=20]
[INFO] [09/07/2017 00:42:10.926] [dj-akka.actor.default-dispatcher-5] [akka://dj/user/carroll] Proceeding with Prove
[INFO] [09/07/2017 00:42:10.930] [dj-akka.actor.default-dispatcher-5] [akka://dj/user/carroll] Proceeding with Prove
[INFO] [09/07/2017 00:42:10.945] [dj-akka.actor.default-dispatcher-4] [akka://dj/user/alice] Message1 received, sending challenge List(-75, 102, -9, 22, -56)
[INFO] [09/07/2017 00:42:10.945] [dj-akka.actor.default-dispatcher-5] [akka://dj/user/bob] Message1 received, sending challenge List(-39, -93, 42, -80, -48)
[INFO] [09/07/2017 00:42:10.946] [dj-akka.actor.default-dispatcher-4] [akka://dj/user/carroll/$b] Resolving challenge edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.SigmaDJProductSecondMsg@110a3cd5
[INFO] [09/07/2017 00:42:10.946] [dj-akka.actor.default-dispatcher-2] [akka://dj/user/carroll/$a] Resolving challenge edu.biu.scapi.interactiveMidProtocols.sigmaProtocol.damgardJurikProduct.SigmaDJProductSecondMsg@61f6f43f
[INFO] [09/07/2017 00:42:10.955] [dj-akka.actor.default-dispatcher-3] [akka://dj/user/alice] ProverResult(true)
[INFO] [09/07/2017 00:42:10.955] [dj-akka.actor.default-dispatcher-5] [akka://dj/user/bob] ProverResult(true)
(ProverResult(true),ProverResult(true))

```

`sbt test`  Runs tests covering basic scenarios

 