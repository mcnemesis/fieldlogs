from django.http import HttpResponse
from fieldlogs.settings import *
from logs.models import *
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render_to_response
import json
import logging
import datetime

logging.basicConfig(level=LOG_LEVEL, filename=MAIN_LOG)
logger = logging.getLogger(__name__)


def j_response(payload, msg, status):
    return HttpResponse( json.dumps( {
        'payload' : payload,
        'message' : msg,
        'status' : status
        }, indent=2), status = status)

def j_error(msg):
    logger.error(msg)
    return j_response(None, msg, 403)

def j_ok(payload, msg):
    logger.info(msg)
    return j_response(payload, msg, 200)

@csrf_exempt
def api_add_log(request):
    if request.method != 'POST':
        return j_error('HTTP VERB Not Supported')


    def p(key):
        return request.POST.get(key,None) if key in request.POST else None
    def f(key):
        return request.FILES.get(key,None) if key in request.FILES else None
    def e(key):
        return True if key in request.POST else False
    def fe(key):
        return True if key in request.FILES else False

    required = ['lat','lng','sub','nat','dob']

    if reduce(lambda u,v: u and v, map(e,required), True) and fe('sig'):
        log = Log(
                latitude = float( p('lat') ),
                longitude = float( p('lng') ),
                subject = p('sub'),
                nationality = p('nat'),
                birthdate = datetime.datetime.strptime( p('dob'), '%Y-%m-%d'),
                location = p('loc'),
                comment = p('com'),
                barcode = p('code'),
                photo_caption = p('cap'),
                signature = f('sig'),
                photo = f('img')
                )
        log.save()
        return j_ok( log.to_dict(), 'Log : %s' % log.id)
    else:
        return j_error('Required: sig, %s' % ', '.join(required))
