#!/bin/bash

# doop runs for commons-collections4
for n in NORMAL NO_INTERPROC; do
  ./doop -a context-insensitive -i $HOME/Benchmarks/commons-collections4-4.4/target/commons-collections4-4.4.jar -i $HOME/Benchmarks/commons-collections4-4.4/target/commons-collections4-4.4-tests.jar -i $HOME/Benchmarks/commons-collections4-4.4/mvn_dependencies --id commons-collections4-mock-counts-$n --souffle-jobs 32 --main org.apache.commons.collections4.RootDriver --define-cpp-macro $n --extra-logic souffle-logic/analyses/mocks/mocks.dl &> $HOME/results/commons-collection4-results/commons-collection4-$n.log
  ./count.py --file results/commons-collections4-4.4/context-insensitive/java_8/commons-collections4-mock-counts-$n/isMockInvocation.csv &> $HOME/results/commons-collection4-results/current-counts-$n
done


# doop runs for jsonschema2pojo-1.1.1-core
for n in NORMAL NO_INTERPROC; do
  ./doop -a context-insensitive -i $HOME/Benchmarks/jsonschema2pojo-1.1.1/jsonschema2pojo-core/target/jsonschema2pojo-core-1.1.1.jar -i $HOME/Benchmarks/jsonschema2pojo-1.1.1/jsonschema2pojo-core/target/jsonschema2pojo-core-1.1.1-tests.jar -i $HOME/Benchmarks/jsonschema2pojo-1.1.1/jsonschema2pojo-core/mvn_dependencies --id jsonschema2pojo-mock-counts-$n --souffle-jobs 32 --main org.jsonschema2pojo.RootDriver --define-cpp-macro $n --extra-logic souffle-logic/analyses/mocks/mocks.dl &> $HOME/results/jsonschema2pojo-core-results/jsonschema2pojo-core-$n.log
  ./count.py --file results/jsonschema2pojo-core-1.1.1/context-insensitive/java_8/jsonschema2pojo-mock-counts-$n/isMockInvocation.csv &> $HOME/results/jsonschema2pojo-core-results/current-counts-$n
done
