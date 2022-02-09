#!/usr/bin/python3

import csv
import re
import argparse, os

path = "../RawResults/"

benchmarks = ['bootique', 'commons-collections4', 'flink-core', 'jsonschema2pojo', 'maven-core', 'microbenchmark', 'mybatis', 'quartz-core', 'vraptor']

benchmarkRootPackageDict = {
  "bootique": "io.bootique",
  "commons-collections4": "org.apache.commons.collections4",
  "flink-core": "org.apache.flink",
  "jsonschema2pojo": "org.jsonschema2pojo",
  "maven-core": "org.apache.maven",
  "microbenchmark": "ca.liang",
  "mybatis": "org.apache.ibatis",
  "quartz-core": "org.quartz",
  "vraptor": "br.com.caelum.vraptor"
}
#benchmarks = ['microbenchmark']
base_analysis = ['basic-only', 'CI', 'CIPP', '1-object-sensitive']
call_graph_file = 'CallGraphEdge.csv'


target_clazz_set = set()
total_target_clazz_counter = 0

def dir_file(string):
    if os.path.isfile(string):
        return string
    else:
        raise FileNotFoundError(string)

with open('CallGraphEdgeTargetClassCounts.csv', mode='w') as outfile:
    writer = csv.writer(outfile)
    header = ['Benchmark', 'basic-only (Target)', 'CI (Target)', 'CIPP (Target)', '1-object-sens (Target)']
    writer.writerow(header)
    for benchmark in benchmarks:
        content_to_print = [benchmark]
        print("Benchmark: {}".format(benchmark))
        for analysis in base_analysis:
            curr_file = path + benchmark + "/" + analysis + "/" + call_graph_file
            target_clazz_set = set()
            total_target_clazz_counter = 0
            if os.path.isfile(curr_file):
                with open(curr_file, newline='') as csvfile:
                    reader = csv.reader(csvfile, delimiter='\t')
                    for row in reader:
                        a, source, b, target = row
                        source_method = source.split('/')[0]
                        source_clazz = source_method.replace("<", "").split(":")[0]

                        target_clazz = target.replace("<", "").split(":")[0]
                        #print("Target Class: {}".format(target_clazz))

                        if target_clazz not in target_clazz_set:
                            target_clazz_set.add(target_clazz)
                            total_target_clazz_counter = total_target_clazz_counter + 1
                print("Base analysis: {}".format(analysis))
                print("total class counter: {}".format(total_target_clazz_counter))
                content_to_print.append(total_target_clazz_counter)
            else:
                content_to_print.append(0)
        writer.writerow(content_to_print)
