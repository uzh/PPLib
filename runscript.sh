for i in `seq 1 100`
do
  nice -n 10 java -jar target/scala-2.11/pplib-assembly-0.1-SNAPSHOT.jar runall answers$1 hostname`hostname`_$i reps$2
done
