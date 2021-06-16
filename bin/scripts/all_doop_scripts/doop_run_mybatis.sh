#!/bin/bash

# doop runs for mybatis
for n in NORMAL NO_INTERPROC; do
  ./doop -a context-insensitive -i $HOME/Benchmarks/mybatis-3-mybatis-3.5.6/target/mybatis-3.5.6.jar -i $HOME/Benchmarks/mybatis-3-mybatis-3.5.6/target/mybatis-3.5.6-tests.jar -i $HOME/Benchmarks/mybatis-3-mybatis-3.5.6/mvn_dependencies --id mybatis-mock-counts-$n --souffle-jobs 32 --main org.apache.ibatis.RootDriver --define-cpp-macro $n --extra-logic souffle-logic/analyses/mocks/mocks.dl &> $HOME/results/mybatis3-results/mybatis3-$n.log
  ./count.py --file results/mybatis-3.5.6/context-insensitive/java_8/mybatis-mock-counts-$n/isMockInvocation.csv &> $HOME/results/mybatis3-results/current-counts-$n
done
