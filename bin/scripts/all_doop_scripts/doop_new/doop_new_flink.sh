#!/bin/bash

# doop runs for flink
BENCHMARK="flink-release-1.13.0-rc1"
RESULT="flink-core-1.13.0"

./doop -a basic-only -i $HOME/Benchmarks/$BENCHMARK/flink-core/target/$RESULT.jar -i $HOME/Benchmarks/$BENCHMARK/flink-core/target/$RESULT-tests.jar -i $HOME/Benchmarks/$BENCHMARK/flink-core/mvn_dependencies --id flink-core-basic-new-NORMAL --souffle-jobs 32 --main org.apache.flink.RootDriver --define-cpp-macro USE_BASIC --extra-logic souffle-logic/analyses/mocks/mocks.dl &> $HOME/results/flink-core-results/flink-core-basic-new-NORMAL.log

for n in NORMAL NO_INTERPROC; do
  mkdir -p results/$RESULT/basic-only/java_8/

  cp -a last-analysis results/$RESULT/basic-only/java_8/flink-core-basic-new-$n

  /usr/bin/time -o $HOME/souffle-basic-$BENCHMARK-$n souffle -F results/$RESULT/basic-only/java_8/flink-core-basic-new-$n -M $n souffle-logic/analyses/mocks/mocks-after.dl -D results/$RESULT/basic-only/java_8/flink-core-basic-new-$n/

  ./count.py --file results/$RESULT/basic-only/java_8/flink-core-basic-new-$n/isMockInvocation.csv &> $HOME/results/flink-core-results/basic-new-counts-$n
done

./doop -a context-insensitive -i $HOME/Benchmarks/$BENCHMARK/flink-core/target/$RESULT.jar -i $HOME/Benchmarks/$BENCHMARK/flink-core/target/$RESULT-tests.jar -i $HOME/Benchmarks/$BENCHMARK/flink-core/mvn_dependencies --id flink-core-new-NORMAL --souffle-jobs 32 --main org.apache.flink.RootDriver --define-cpp-macro USE_CALLGRAPH --extra-logic souffle-logic/analyses/mocks/mocks.dl &> $HOME/results/flink-core-results/flink-core-new-NORMAL.log

for n in NORMAL NO_INTERPROC; do
  mkdir -p results/$RESULT/context-insensitive/java_8/

  cp -a last-analysis results/$RESULT/context-insensitive/java_8/flink-core-new-$n

  /usr/bin/time -o $HOME/souffle-context-insensitive-$BENCHMARK-$n souffle -F results/$RESULT/context-insensitive/java_8/flink-core-new-$n -M $n souffle-logic/analyses/mocks/mocks-after.dl -D results/$RESULT/context-insensitive/java_8/flink-core-new-$n/

  ./count.py --file results/$RESULT/context-insensitive/java_8/flink-core-new-$n/isMockInvocation.csv &> $HOME/results/flink-core-results/new-counts-$n
done