#1 createdb gaezdb
#2 su - postgres
#3 run -> psql gaezdb < test-data/dumpGaezprj04012012.backup 
run query to select ry

<--setup of geobatch -->

corepool size: 2
maxpool: 5
queuesize: 100

T_asynchCopy~=20secs (for nCopyThreads==5)

size rst (idrisi) == 20Mb ~

NAS to GB_data_dir transfer rate == 11 Mb/sec
T_calc=?

T_csv_move<<1 Secs

T_calc= n*10 ~ Secs

1 flow ex (for limitSize == nCopyThreads == 5) < (T_asynchCopy + T_calc + T_csv_move)*5

(if T_calc==40 Secs) ->  T_polling < 3.6 min

0 0/3 * * * *.-. cron quartz scheduler
0 0/1 * * * *.-. cron quartz scheduler [PRODUCTION]
0/30 * * * * *.-. cron quartz scheduler [TEST]