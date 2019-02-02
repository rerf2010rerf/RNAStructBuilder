###Command line
java -jar RNAStructBuilder.jar [-optionName:optionValue]

For example: 

java -jar RNAStructBuilder.jar -rscanDir:c:\rscan\ -rscanConfigFile:c:\rscan\rscan.cfg -logFile:log.txt -resultFile:result.pat -seqFile:c:\temp.fa -enableOpenedEnding

####Options:
-rscanDir: the directory containing executable of the RScan program. 

-rscanConfigFile: the configuration file of RScan which is passed through -o argument of RScan

-initialPatternFile: pattern file used for initiate of simulated annealing. It should be in RScan PAT-file format

-rscanTimeLimit: maximum duration of computation of RScan at each simulated annealing step

-seqFile: sequences' set in fasta format

-minSeqs: the minimum number of sequences from the set to be covered by the constructed model

-iterationToStop: the number of iteration of the simulated annealing without changing the model after which algorithm should be stopped

-temperatureKoeff: temperature coefficient in simulated annealing

-energyKoeff: energy coefficient the of cost function

-timeKoeff: time coefficient the of cost function

-numberKoeff: coefficient of number of covered sequences of the cost function

-logFile: file for simulated annealing computation log

-resultFile: file for resulting pattern of secondary structure

-enableOpenedEnding: enables building model for the beginning of the RNA sequences. If this parameter is absent, whole sequence have to covered by model

####RScan help
See RScan http://www.softberry.com/freedownloadhelp/rna/rscan/rscan.all.html
