rm  /var/folders/mj/hzsg5cw55vj17cj9rhszzzsc0000gn/T/consumer.dat
echo running queue $1
echo generating statistic file $2
java -jar ./target/destination-bench.jar --protocol amqp --consumer-sample LossLess --wait 5 --url amqp://localhost:61616 --name $1 --iterations 20000 --runs 5 --warmup 20000
java -jar ./target/statistics-summary-generator.jar --input  /var/folders/mj/hzsg5cw55vj17cj9rhszzzsc0000gn/T/consumer.dat --iterations 20000 --runs 5 --warmup 20000 --format LONG | tee $2

