"""
A command line tool that takes a list of pathnames containing calibration data from tonesetandroid and generates
a frequency response graph and sigmoid graphs for all files. Files must all have same participant ID

NOTE: this version is intended to work with tonesetandroid version 1: the single pure-tone test
"""

import sys
import matplotlib.pyplot as plt
import numpy as np


"""
Reset matplotlib and clear all results from plot
"""
def reset_plot():
    plt.cla()
    plt.clf()
    plt.close()


"""
Reads the file at the given path, returns a tuple of (subject ID, noise type, abs_path, dict mapping frequencies to
tuple of (vol, P(heard)))
"""
def read_csv(abs_path):
    with open(abs_path, 'r') as f:
        info_line = f.readline().split(' ')
        sub_id = int(info_line[1])  # get subject id
        noise_type = str(info_line[3]) # get noise type
        f.readline()  # skip header
        freq_results = {}  # map frequencies to tuple = (vol, P(heard))
        for line in f.readlines():
            line = line.split(',')
            freq = float(line[0])
            vol = float(line[1])
            n_heard = int(line[2])
            n_not_heard = int(line[3])
            total = n_heard + n_not_heard
            p = float(n_heard) / float(total)
            if freq in list(freq_results):
                freq_results[freq].append((vol, p))
            else:
                freq_results[freq] = [(vol, p)]
        return sub_id, noise_type, abs_path, freq_results


"""
Generate and save a graph plotting the 'sigmoid' curves of each frequency tested in freq_results
"""
def generate_sig_graph(freq_results):
    sub_id = freq_results[0]         # parse output from read_csv
    noise_type = freq_results[1]
    pathname = freq_results[2][0:-4] # pathname minus '.csv'
    freq_results = freq_results[3].copy()
    del freq_results[200]  # remove line for 200Hz because it looks awful

    line_color_args = ["ro-", "yo-", "bo-", "go-", "mo-", "co-", "ko-"]  # arrange data
    freq_list = []
    x_data = []
    y_data = []
    for freq in list(freq_results):
        pairs = freq_results[freq]
        freq_list.append(freq)
        new_x_list = []
        new_y_list = []
        for pair in pairs:
            new_x_list.append(pair[0])
            new_y_list.append(pair[1])
        x_data.append(new_x_list)
        y_data.append(new_y_list)

    max_x = float(max([max(lst) for lst in x_data]))        # aesthetics
    x_ticks = np.arange(0, max_x, 100)
    y_ticks = np.arange(0, 1.1, 0.1)
    plt.xticks(x_ticks)
    plt.yticks(y_ticks)
    plt.figure(figsize=(12, 6))
    plt.xlabel("Volume")
    plt.ylabel("P(heard)")
    plt.title("Participant " + str(sub_id) + " " + noise_type + " Calibration")

    for i in range(len(x_data)):            # plot
        plt.plot(x_data[i], y_data[i], line_color_args[i], label=str(freq_list[i]) + " Hz")

    plt.legend(title="Frequency")
    plt.savefig(pathname + "_" + noise_type + "_sigplot")  # use same name as calibration file
    reset_plot()


"""
Generate and save a graph containing the lowest volume with P(heard) >= 0.9 for each frequency in result, for each
result in freq_results_list
"""
def generate_res_graph(freq_results_list):
    x_data = []
    y_data = []
    noise_type_lst = []
    first_sub_id = freq_results_list[0][0]
    line_color_args = ["ro-", "bo-", "go-", "mo-", "yo-", "co-", "ko-"]

    for freq_results in freq_results_list:
        if freq_results[0] != first_sub_id:     # make sure all have same subject ID
            raise AssertionError

        noise_type = freq_results[1]            # parse freq_results
        freq_results = freq_results[3].copy()
        if 200 in freq_results.keys():
            del freq_results[200]  # remove point for 200Hz because it looks awful

        min_vols = []                               # find minimum volume with P(heard) >= 0.8 for each frequency
        for freq in sorted(freq_results.keys()):    # and update lists with data
            min_vol = 100000000
            fvp_list = freq_results[freq]
            for fvp in fvp_list:
                if fvp[1] >= 0.8 and fvp[0] < min_vol:
                    min_vol = fvp[0]
            if min_vol == 100000000:  # do not plot if no 80% volume found
                continue
            else:
                min_vols.append((freq, min_vol))
        x_data.append([tup[0] for tup in min_vols])
        y_data.append([tup[1] for tup in min_vols])
        noise_type_lst.append(noise_type)

    max_x = max([max(lst) for lst in x_data])   # aesthetics
    max_y = max([max(lst) for lst in y_data])
    x_ticks = np.arange(0, max_x + 100, 100)
    y_ticks = np.arange(0, max_y + 10, 10)
    plt.xticks(x_ticks)
    plt.yticks(y_ticks)
    plt.figure(figsize=(12, 8))
    plt.xlabel("Frequency (Hz)")
    plt.ylabel("80% Volume")
    plt.title("Participant " + str(first_sub_id) + " Frequency Response")

    for i in range(len(x_data)):                # plot line for each noise type
        plt.plot(x_data[i], y_data[i], line_color_args[i], label=noise_type_lst[i])

    plt.legend(title="Noise Type")              # save plot as .png
    plt.savefig("sub" + str(first_sub_id) + "_resplot")
    reset_plot()


if __name__ == "__main__":
    if len(sys.argv) >= 2:
        freq_results_list = []
        for filepath in sys.argv[1:]:
            try:                                                            # read files
                print("reading file " + filepath)
                freq_results_list.append(read_csv(filepath))
            except FileNotFoundError:
                print("File not found: aborted")
        try:                                                                # generate response graph for file
            print("generating response graph")
            generate_res_graph(freq_results_list)
        except AssertionError:
            print("Files do not all have same subject ID: aborted")

        for freq_results in freq_results_list:                              # generate sigmoid graph for each file
            print("generating sigmoid graph for " + freq_results[2])
            generate_sig_graph(freq_results)

    else:
        print("A command line tool that takes a list of pathnames containing calibration data from tonesetandroid and "
              "generates a frequency response graph and sigmoid graphs for all files. Files must all have same "
              "participant ID")
        print("Usage: " + sys.argv[0] + " absolute/file/path/1.csv absolute/file/path/2.csv ...")
