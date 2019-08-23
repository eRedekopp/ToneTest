
import matplotlib.pyplot as plt
import scipy.optimize as opt
import sys as sys
import math as math


def plot(filename):
    with open(filename) as f:
        colours = ['b', 'g', 'c', 'm', 'y', 'k']
        colouridx = 0
        xvals = f.readline().rstrip("\n")
        xvals = xvals.split(",")
        xvals = [float(n) for n in xvals]
        xplots = []
        yplots = []
        for line in f.readlines():
            line = line.rstrip("\n")
            line = line.split(",")
            for i in range(len(line)):
                if line[i] == '':
                    line[i] = '-1.0'
            line = [float(n) for n in line]

            xplot = []
            yplot = []
            for i in range(len(xvals)):
                if line[i] != -1:
                    xplot.append(xvals[i])
                    yplot.append(line[i])
            xplots.append(xplot)
            yplots.append(yplot)
            print("plotting \nx = " + str(xplot) + "\ny = " + str(yplot))
            plt.plot(xplot, yplot, colours[colouridx] + "-")
            if colouridx < len(colours) - 1:
                colouridx += 1
            else:
                colouridx = 0

    xdata = []
    ydata = []
    for x in range(1000):
        x /= 1000
        xdata.append(x)
        ydata.append(math.log((math.e - 1) * x + 1))

    plt.plot(xdata, ydata, color=(0, 0, 0), linestyle="--", linewidth=4, label="y=ln((e - 1)x + 1)")
    plt.plot([0.0, 1.0], [0.0, 1.0], "r--", linewidth=4, label="y = x")
    # optimum = fit_sigmoid(xplots, yplots)
    # plt.plot(optimum[0], optimum[1], color=(0, 0, 0), linestyle="--", linewidth=5, label="y = 1 / (1 + e ^ -" + str(optimum[2]) + "x)")
    plt.xlim([0, 1])
    plt.ylim([0, 1])
    plt.xlabel("Distance between 0% and 100% volumes")
    plt.ylabel("P(Heard)")
    plt.title("P(Heard) as a position between 0% and 100% volumes")
    plt.legend()
    plt.show()


# def sigmoid(x, a):
#     return 1.0 / (1.0 + math.e ** (-1 * (x - 0.5) * a))  # = 1 / (1 + e^-a(x - 1/2))
#
#
# """
# Return 2 lists full of xdata and y data for a bunch of data points in [0, 1] from an optimized sigmoid, and the optimized value of a
# """
# def fit_sigmoid(xplots, yplots):
#     xdata = [datum for lst in xplots for datum in lst]  # flatten lists
#     ydata = [datum for lst in yplots for datum in lst]
#
#     popt = opt.curve_fit(sigmoid, xdata, ydata)[0]
#     print("popt = " + str(popt))
#     a = popt[0]
#     xopt = []
#     yopt = []
#     for x in range(0, 1000):
#         x = float(x) / 1000.0  # calculate for every 0.001
#         xopt.append(x)
#         yopt.append(sigmoid(x, a))
#
#     return xopt, yopt, a


if __name__ == "__main__":
    if len(sys.argv) == 2:
        plot(sys.argv[1])
    else:
        print("Usage: " + sys.argv[0] + " filename")
