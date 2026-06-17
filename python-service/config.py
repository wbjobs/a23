import os

BASE_DIR = os.path.abspath(os.path.dirname(__file__))

class Config:
    DEBUG = os.environ.get('FLASK_DEBUG', True)
    HOST = os.environ.get('FLASK_HOST', '0.0.0.0')
    PORT = int(os.environ.get('FLASK_PORT', 5000))
    
    TEMP_DIR = os.path.join(BASE_DIR, 'temp')
    MAX_CONTENT_LENGTH = 100 * 1024 * 1024
    
    GROBID_URL = os.environ.get('GROBID_URL', 'http://localhost:8070')
    GROBID_TIMEOUT = 60
    
    ALLOWED_EXTENSIONS = {'pdf'}
