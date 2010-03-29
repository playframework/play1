
class Downloader(object):
	before = .0
	history = []
	cycles = 0
	average = lambda self: sum(self.history) / (len(self.history) or 1)

	def __init__(self, width=55):
		self.width = width
		self.kibi = lambda bits: bits / 2 ** 10
		self.proc = lambda a, b: a / (b * 0.01)

	def retrieve(self, url, destination, callback=None):
		self.size = 0
		time.clock()
		try: urllib.urlretrieve(url, destination, self.progress)
		except KeyboardInterrupt:
			print '\n~ Download cancelled'
			print '~'
			for i in range(5):
				try:
					os.remove(destination)
					break
				except:
					time.sleep(.1)
			else: raise
			if callback: callback()
			sys.exit()
		print ''
		return self.size

	def progress(self, blocks, blocksize, filesize):
		self.cycles += 1
		bits = min(blocks*blocksize, filesize)
		done = self.proc(bits, filesize) if bits != filesize else 100
		bar = self.bar(done)
		if not self.cycles % 3 and bits != filesize:
			now = time.clock()
			elapsed = now-self.before
			if elapsed:
				speed = self.kibi(blocksize * 3 / elapsed)
				self.history.append(speed)
				self.history = self.history[-4:]
			self.before = now
		average = round(sum(self.history[-4:]) / 4, 1)
		self.size = self.kibi(bits)
		print '\r~ [%s] %s KiB/s  ' % (bar, str(average)),

	def bar(self, done):
		span = self.width * done * 0.01
		offset = len(str(int(done))) - .99
		result = ('%d%%' % (done,)).center(self.width)
		return result.replace(' ', '-', int(span - offset))

class Unzip:
	def __init__(self, verbose = False, percent = 10):
		self.verbose = verbose
		self.percent = percent

	def extract(self, file, dir):
		if not dir.endswith(':') and not os.path.exists(dir):
			os.mkdir(dir)
		zf = zipfile.ZipFile(file)
		# create directory structure to house files
		self._createstructure(file, dir)
		num_files = len(zf.namelist())
		percent = self.percent
		divisions = 100 / percent
		perc = int(num_files / divisions)
		# extract files to directory structure
		for i, name in enumerate(zf.namelist()):
			if self.verbose == True:
				print "Extracting %s" % name
			elif perc > 0 and (i % perc) == 0 and i > 0:
				complete = int (i / perc) * percent
			if not name.endswith('/'):
				outfile = open(os.path.join(dir, name), 'wb')
				outfile.write(zf.read(name))
				outfile.flush()
				outfile.close()
