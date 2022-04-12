import json
import redis
import os
import subprocess
from redis import Sentinel, Redis

key_prefix = os.getenv('KEY_PREFIX', 'encore')
queue_name = key_prefix + '-childjob-queue'
progress_queue_name = key_prefix + '-childjob-progress-queue'
#queue_name = key_prefix + 'childjob-queue'

sentinel_host = os.getenv('REDIS_SENTINEL_HOST')
sentinel_port = os.getenv('REDIS_SENTINEL_PORT', 26379)

redis_host = os.getenv('REDIS_HOST', 'localhost')
redis_port = os.getenv('REDIS_PORT', 6379)
master = None
if sentinel_host is not None:
    sentinel = Sentinel([(sentinel_host, sentinel_port)], socket_timeout=0.1)
    master = sentinel.master_for('mymaster', socket_timeout=0.1)
else:
    master = Redis(host = redis_host, port = redis_port, decode_responses=True)

def get_queue_item():
    queue_item_str = master.lpop(queue_name)
    if queue_item_str is None:
        return None
    return json.loads(queue_item_str)

def get_child_job(id):
    child_job = master.hgetall('encore-child-jobs:' + id)
    if child_job is None:
        return None
    cmds_deser = json.loads(child_job['commands'])
    child_job['commands'] = cmds_deser
    return child_job

def run_command(cmd):
    p = subprocess.Popen(cmd, stderr=subprocess.PIPE, encoding="utf8")
    while True:
        line = p.stderr.readline()
        if not line:
            break
        print("LINE: " + line, end='')
    ret = p.wait(60)
    if ret != 0:
        raise Exception('process returned non-zero exit code: ' + ret)

def update_child_job(child_job, status, progress):
    child_job['status'] = status
    child_job['progress'] = progress
    master.hset(child_job['key'], mapping = {'status': status, 'progress': progress})
    
def child_job_failed(child_job):
    update_child_job(child_job, 'FAILED', 100)

def child_job_success(child_job):
    update_child_job(child_job, 'SUCCESSFUL', 100)

def update_active_child_jobs(child_job):
    return master.hincrby(child_job['parentKey'], 'activeChildJobs', -1)

def send_progress(child_job, activeChildJobs):
    progress = {
        '@class': 'se.svt.oss.encore.model.childjob.ChildJobProgress',
        'childJobId': child_job['id'],
        'parentId': child_job['parentId'],
        'progress': child_job['progress'],
        'status': child_job['status'],
        'activeChildJobs': activeChildJobs}
    master.rpush(progress_queue_name, json.dumps(progress))

queue_item = get_queue_item()
print(queue_item)
child_job = get_child_job(queue_item['id'])
print(child_job)
try:
    for cmd in child_job['commands']:
        run_command(cmd)
    child_job_success(child_job)
    activeChildJobs = update_active_child_jobs(child_job)
    send_progress(child_job, activeChildJobs)
except Exception:
    child_job_failed(child_job)
    send_progress(child_job, activeChildJobs)
    
