// Reference only. This is the state + layout logic of the clickable prototype (night palette).
// Port the ideas, not the syntax. Layout numbers (radii, sizes, angles) are the source of truth.
// Coordinates here are CSS pixels on a 390x844 artboard; treat 1px as 1dp in Compose.


const ACC='#E8A87C', ONACC='#181310', BG='#12151A', SURF='#171B22', TXT='#F1F0EC', MUT='#7C828E', BRD='#21252E', TRACK='#262B35';
const PEOPLE = {
  AO:['Amara Osei','#D98C4A'], PK:['Priya Kapoor','#7FA88C'], JL:['Jonah Lee','#8DA6D9'], TN:['Tomas Novak','#3B4351'],
  RS:['Riya Sharma','#C77B8F'], MD:['Marco Diaz','#6FA8B5'], EK:['Elin Kaur','#3B4351'], SY:['Sana Yildiz','#E9C46A'],
  NB:['Nils Baum','#7FA88C'], LF:['Lena Fuchs','#8DA6D9'], OH:['Omar Haddad','#D98C4A'], CZ:['Clara Zeller','#C77B8F'], S:['You','#3B4351']
};
const fgOf = (c) => { const r=parseInt(c.slice(1,3),16), g=parseInt(c.slice(3,5),16), b=parseInt(c.slice(5,7),16); return (0.299*r+0.587*g+0.114*b) < 110 ? '#F1F0EC' : '#181310'; };
const FEED = [
  { id:'n0', ini:'AO', when:'2h', text:"Finally finished the puzzle we started at Mom's over the summer.", thread:true },
  { id:'n1', ini:'PK', when:'5h', text:'Sea glass hunting turned into a two hour walk. Worth it.', photo:true },
  { id:'n2', ini:'JL', when:'yesterday', text:'Some days the good coffee is the whole plan.' },
  { id:'n3', ini:'TN', when:'yesterday', text:'Officially declared the tomatoes a lost cause. Onward to garlic.' },
  { id:'n4', ini:'RS', when:'yesterday', text:'My first sourdough that actually rose. Telling everyone.' }
];
const GFEED = [
  { g:'Aachen Photo Walk', gl:'A', gc:'#D98C4A', ini:'PK', when:'40m', text:'Golden hour shoot this Sunday. Meet at the cathedral steps, 6:30 sharp.' },
  { g:'RWTH Board Game Club', gl:'R', gc:'#8DA6D9', ini:'JL', when:'2h', text:'Anyone up for Catan tonight? Need a fourth.' },
  { g:'Sunday Trail Runners', gl:'S', gc:'#7FA88C', ini:'AO', when:'5h', text:'Route for this week is up. Easy pace, brunch after.' }
];
const P12 = ['AO','PK','JL','TN','RS','EK','MD','SY','NB','LF','OH','CZ'];
const SINCE = ['2023','2023','2023','2024','2024','2024','2024','2025','2025','2025','2025','2026'];
const EVENTS = [
  { t:'Trail Run + Brunch', d:'19', m:'SEP', phi:40, topic:'OUTDOORS', when:'Sat 19 Sep, 9:00', where:'Forest Park Trailhead', going:4, cap:10, vis:'OPEN NEARBY', host:'PK' },
  { t:'Board Game Night', d:'18', m:'SEP', phi:125, topic:'SOCIAL', when:'Fri 18 Sep, 18:00', where:'Kaffeehaus Zeitlos', going:6, cap:12, vis:'FRIENDS OF FRIENDS', host:'PK' },
  { t:'Studio Open Mic', d:'21', m:'SEP', phi:215, topic:'MUSIC', when:'Mon 21 Sep, 20:30', where:'MTI Courtyard', going:3, cap:20, vis:'FRIENDS', host:'JL' },
  { t:'Market Walk', d:'20', m:'SEP', phi:165, topic:'SOCIAL', when:'Sun 20 Sep, 10:00', where:'Elisenbrunnen', going:6, cap:12, vis:'OPEN NEARBY', host:'AO' },
  { t:'Gallery Night', d:'22', m:'SEP', phi:305, topic:'MAKING', when:'Tue 22 Sep, 19:00', where:'Ludwig Forum', going:5, cap:15, vis:'FRIENDS OF FRIENDS', host:'RS' }
];
const GROUPS = [
  { t:'Aachen Photo Walk', l:'A', c:'#D98C4A', phi:322, topic:'PHOTOGRAPHY', members:128 },
  { t:'Sunday Trail Runners', l:'S', c:'#7FA88C', phi:62, topic:'FITNESS & SPORTS', members:128 },
  { t:'RWTH Board Game Club', l:'R', c:'#8DA6D9', phi:138, topic:'GAMING', members:64 },
  { t:'Aachen Choir', l:'C', c:'#C77B8F', phi:248, topic:'MUSIC', members:42 }
];
const MAIN = ['home','messages','explore','alerts','profile'];
const SEATPOOL = ['PK','JL','AO','TN','RS','MD','EK','SY','NB','LF','OH','CZ','PK','JL','AO','TN','RS','MD','EK','SY'];
const THREADS = {
  pk:{ title:'Priya Kapoor', ini:'PK', group:false }, jl:{ title:'Jonah Lee', ini:'JL', group:false }, ao:{ title:'Amara Osei', ini:'AO', group:false },
  g1:{ title:'Aachen Photo Walk', ini:'A', group:true, c:'#D98C4A' }, g2:{ title:'RWTH Board Game Club', ini:'R', group:true, c:'#8DA6D9' }
};
const sinpos = (cx, cy, r, phi) => [cx + r*Math.sin(phi*Math.PI/180), cy - r*Math.cos(phi*Math.PI/180)];

class Component extends DCLogic {
  constructor(props) {
    super(props);
    this.state = {
      screen:'home', stack:[], homeTab:0, liked:{}, posts:[], aud:'Friends', draft:'', c1:false, c2:false, replies:[],
      msgTab:0, thread:'pk', chats:{
        pk:[{m:false,w:'Priya',t:'Sounds good, see you then'},{m:false,w:'Priya',t:'Bringing the tripod?'}],
        jl:[{m:false,w:'Jonah',t:'Bringing the tripod'}], ao:[{m:false,w:'Amara',t:'Thanks for the recipe'}],
        g1:[{m:false,w:'Priya',t:'Meet at the cathedral steps, 6:30'},{m:false,w:'Tomas',t:"I'll bring a tripod to share"}],
        g2:[{m:false,w:'Jonah',t:'Anyone up for Catan tonight?'}]
      },
      notifTab:0, req:{ riya:'pending', marco:'pending' }, inner:[0,1,2,3,4], csel:5, exSel:1, gSel:0, gTab:0,
      gMember:[true,false,false,false], gReq:[false,false,false,false], eSel:1, joined:[false,false,false,false,false]
    };
  }
  go(screen) { this.setState({ screen, stack: this.state.stack.concat([this.state.screen]), draft:'' }); }
  tab(screen) { this.setState({ screen, stack:[], draft:'' }); }
  back() { const s = this.state.stack.slice(); const prev = s.pop() || 'home'; this.setState({ screen: prev, stack: s, draft:'' }); }
  say(chatId, text) {
    const chats = Object.assign({}, this.state.chats);
    chats[chatId] = (chats[chatId] || []).concat([{ m:true, w:'You', t:text }]);
    this.setState({ chats, draft:'' });
    setTimeout(() => {
      const c2 = Object.assign({}, this.state.chats);
      c2[chatId] = (c2[chatId] || []).concat([{ m:false, w: THREADS[chatId].group ? 'Priya' : THREADS[chatId].title.split(' ')[0], t: THREADS[chatId].group ? 'Count me in.' : 'Sounds good!' }]);
      this.setState({ chats: c2 });
    }, 1300);
  }
  renderVals() {
    const s = this.state, v = {};
    const sc = s.screen;
    ['home','messages','explore','alerts','profile','circle','thread','compose','group','event','chat'].forEach(k => { v['is' + k[0].toUpperCase() + k.slice(1)] = sc === k; });
    v.showNav = MAIN.indexOf(sc) >= 0;
    const navTo = ['home','messages','explore','alerts','profile'];
    navTo.forEach((k, i) => { v['nav'+i] = () => this.tab(k); v['na'+i] = (MAIN.indexOf(sc) === i) ? ACC : MUT; });
    v.back = () => this.back();
    v.draft = s.draft; v.onDraft = (e) => this.setState({ draft: e.target.value });

    // ---------- HOME
    v.hfLine = s.homeTab === 0 ? ACC : 'transparent'; v.hgLine = s.homeTab === 1 ? ACC : 'transparent';
    v.hfColor = s.homeTab === 0 ? TXT : MUT; v.hgColor = s.homeTab === 1 ? TXT : MUT;
    v.homeFriends = () => this.setState({ homeTab: 0 }); v.homeGroups = () => this.setState({ homeTab: 1 });
    v.feedFriends = s.homeTab === 0; v.feedGroups = s.homeTab === 1;
    v.goCompose = () => this.go('compose');
    const mk = (n, mine) => {
      const p = PEOPLE[n.ini], liked = !!s.liked[n.id];
      return { id:n.id, ini:n.ini, name:p[0], col:p[1], fg:fgOf(p[1]), whenText:'\u00b7 ' + n.when, text:n.text, photo:!!n.photo, tag:n.tag || '',
        hasTag: !!n.tag, hf: liked ? ACC : 'none', hs: liked ? ACC : MUT,
        toggleLike: () => { const l = Object.assign({}, this.state.liked); l[n.id] = !l[n.id]; this.setState({ liked: l }); },
        openThread: () => { if (n.thread) this.go('thread'); } };
    };
    v.feed = s.posts.map(p => mk(p)).concat(FEED.map(n => mk(n)));
    v.gfeed = GFEED.map(g => { const p = PEOPLE[g.ini]; return { g:g.g, gl:g.gl, gc:g.gc, ini:g.ini, col:p[1], fg:fgOf(p[1]), name:p[0], whenText:'\u00b7 ' + g.when, text:g.text }; });

    // ---------- EXPLORE
    const CX=195, CY=170, RI=68, RO=124;
    const items = EVENTS.map((e,i) => ({ k:'e', i, e })).concat(GROUPS.map((g,j) => ({ k:'g', i:EVENTS.length + j, g })));
    v.xnodes = items.map(o => {
      const sel = s.exSel === o.i, pos = o.k === 'e' ? sinpos(CX,CY,RI,o.e.phi) : sinpos(CX,CY,RO,o.g.phi);
      const w = o.k === 'e' ? 40 : 36;
      return { x:(pos[0]-w/2).toFixed(1), y:(pos[1]-w/2).toFixed(1), w, rad: o.k === 'e' ? '50%' : '11px',
        bg: o.k === 'e' ? SURF : o.g.c, bd: o.k === 'e' ? '1.5px solid ' + ACC : 'none', isE: o.k === 'e',
        notE: o.k !== 'e', l1: o.k === 'e' ? o.e.d : '', l2: o.k === 'e' ? o.e.m : '', letter: o.k === 'g' ? o.g.l : '',
        sh: sel ? '0 0 0 3px ' + BG + ', 0 0 0 5px ' + ACC : 'none', scale: sel ? 1.18 : 1, pick: () => this.setState({ exSel: o.i }) };
    });
    const sel = items[s.exSel];
    if (sel.k === 'e') { v.xKicker = 'EVENT \u00b7 ' + sel.e.topic; v.xTitle = sel.e.t; v.xMeta = sel.e.when + ' \u00b7 ' + sel.e.where; v.xIsEvent = true;
      const gg = sel.e.going + (s.joined[s.exSel] ? 1 : 0); v.xPct = Math.round(gg / sel.e.cap * 100); v.xCap = gg + ' of ' + sel.e.cap + ' going'; v.xCta = 'View event'; }
    else { v.xKicker = 'GROUP \u00b7 ' + sel.g.topic; v.xTitle = sel.g.t; v.xMeta = sel.g.members + ' members, public'; v.xIsEvent = false; v.xPct = 0; v.xCap = ''; v.xCta = 'View group'; }
    v.xOpen = () => { if (sel.k === 'e') { this.setState({ eSel: s.exSel }); } else { this.setState({ gSel: s.exSel - EVENTS.length, gTab: 0 }); } this.go(sel.k === 'e' ? 'event' : 'group'); };

    // ---------- MESSAGES
    v.mtDm = s.msgTab === 0; v.mtGroup = s.msgTab === 1;
    v.mdLine = s.msgTab === 0 ? ACC : 'transparent'; v.mgLine = s.msgTab === 1 ? ACC : 'transparent';
    v.mdColor = s.msgTab === 0 ? TXT : MUT; v.mgColor = s.msgTab === 1 ? TXT : MUT;
    v.msgDm = () => this.setState({ msgTab: 0 }); v.msgGroup = () => this.setState({ msgTab: 1 });
    const last = (id) => { const c = s.chats[id]; return c[c.length-1].t; };
    const row = (id, time, group) => { const th = THREADS[id], p = PEOPLE[th.ini]; return { name:th.title, prev:last(id), time, ini:th.ini, col: group ? th.c : p[1], fg: fgOf(group ? th.c : p[1]), rad: group ? '14px' : '50%', open: () => { this.setState({ thread:id }); this.go('chat'); } }; };
    v.dmRows = [row('pk','10m'), row('jl','2h'), row('ao','1d')];
    v.grRows = [row('g1','12m',true), row('g2','2h',true)];
    const th = THREADS[s.thread]; v.chatTitle = th.title;
    v.bubbles = (s.chats[s.thread] || []).map(b => ({ mine:b.m, text:b.t, who:b.w, align: b.m ? 'flex-end' : 'flex-start', bg: b.m ? ACC : SURF, fg: b.m ? ONACC : TXT, bd: b.m ? ACC : BRD, rad: b.m ? '16px 16px 4px 16px' : '16px 16px 16px 4px' }));
    v.sendChat = () => { const t = (s.draft || '').trim(); if (t) this.say(s.thread, t); };

    // ---------- ALERTS
    v.ntNotif = s.notifTab === 0; v.ntAct = s.notifTab === 1;
    v.nnLine = s.notifTab === 0 ? ACC : 'transparent'; v.naLine = s.notifTab === 1 ? ACC : 'transparent';
    v.nnColor = s.notifTab === 0 ? TXT : MUT; v.naColor = s.notifTab === 1 ? TXT : MUT;
    v.notifTabNotif = () => this.setState({ notifTab: 0 }); v.notifTabAct = () => this.setState({ notifTab: 1 });
    const REQ = [{ id:'riya', ini:'RS', sub:'3 mutual friends' }, { id:'marco', ini:'MD', sub:'1 mutual friend' }];
    v.reqs = REQ.filter(r => s.req[r.id] !== 'declined').map(r => { const p = PEOPLE[r.ini], st = s.req[r.id];
      return { name:p[0], ini:r.ini, col:p[1], fg:fgOf(p[1]), pending: st === 'pending', accepted: st === 'accepted', sub: st === 'accepted' ? 'Now friends' : r.sub,
        accept: () => this.setState({ req: Object.assign({}, this.state.req, { [r.id]: 'accepted' }) }),
        decline: () => this.setState({ req: Object.assign({}, this.state.req, { [r.id]: 'declined' }) }) }; });
    v.hasReqs = v.reqs.length > 0;
    const acc = (s.req.riya === 'accepted' ? 1 : 0) + (s.req.marco === 'accepted' ? 1 : 0);
    v.friendCount = 128 + acc;
    v.recent = [['PK','liked your post','10m'],['JL','commented: "Where was this?"','25m'],['AO','reposted your thought','1h'],['TN','quoted your post: "so real"','3h']].map(r => { const p = PEOPLE[r[0]]; return { name:p[0], col:p[1], fg:fgOf(p[1]), ini:r[0], act:r[1], time:r[2] }; });
    v.activity = [['PK',"liked Jonah's post",'20m'],['AO',"commented on Tomas's thought",'45m'],['JL','reposted a thought from Riya','2h'],['MD','and Priya are now friends','5h']].map(r => { const p = PEOPLE[r[0]]; return { name:p[0].split(' ')[0], col:p[1], fg:fgOf(p[1]), ini:r[0], act:r[1], time:r[2] }; });

    // ---------- PROFILE / CIRCLE
    v.innerCount = s.inner.length;
    v.openCircle = () => this.go('circle');
    v.myPosts = s.posts.map(p => ({ text:p.text, whenText:'now', tag:p.tag || '', hasTag: !!p.tag }));
    v.hasMyPosts = s.posts.length > 0;
    const inn = [], out = [];
    P12.forEach((ini, i) => (s.inner.indexOf(i) >= 0 ? inn : out).push(i));
    const nodes = [];
    const place = (arr, r, size, off) => arr.forEach((id, k) => {
      const a = (-90 + off + k * 360 / arr.length) * Math.PI / 180, ini = P12[id], p = PEOPLE[ini];
      nodes.push({ id, x:(195 + r*Math.cos(a) - size/2).toFixed(1), y:(170 + r*Math.sin(a) - size/2).toFixed(1), s:size, ini, col:p[1], fg:fgOf(p[1]),
        sh: s.csel === id ? '0 0 0 2px ' + BG + ', 0 0 0 4px ' + ACC : '0 0 0 2px ' + BG, pick: () => this.setState({ csel: id }) });
    });
    place(inn, 64, 38, 0); place(out, 122, 36, 360 / (2 * Math.max(1, out.length)));
    v.cnodes = nodes;
    const me = P12[s.csel], mp = PEOPLE[me], isIn = s.inner.indexOf(s.csel) >= 0;
    v.cIni = me; v.cName = mp[0]; v.cCol = mp[1]; v.cFg = fgOf(mp[1]);
    v.cStatus = (isIn ? 'In your inner circle' : 'Friend') + ' \u00b7 since ' + SINCE[s.csel];
    v.cBtn = isIn ? 'Move back to friends' : 'Move to inner circle';
    v.cBtnBg = isIn ? 'transparent' : ACC; v.cBtnFg = isIn ? TXT : ONACC;
    v.cToggle = () => { const has = s.inner.indexOf(s.csel) >= 0; this.setState({ inner: has ? s.inner.filter(x => x !== s.csel) : s.inner.concat([s.csel]).sort((a,b) => a-b) }); };

    // ---------- THREAD
    v.c1Closed = !s.c1; v.c1Open = s.c1; v.c2Closed = !s.c2; v.c2Open = s.c2;
    v.showC1 = () => this.setState({ c1: true }); v.showC2 = () => this.setState({ c2: true });
    v.replies = s.replies.map(t => ({ text:t }));
    v.sendReply = () => { const t = (s.draft || '').trim(); if (t) this.setState({ replies: s.replies.concat([t]), draft:'' }); };
    v.threadLiked = !!s.liked['n0']; v.threadHf = s.liked['n0'] ? ACC : 'none'; v.threadHs = s.liked['n0'] ? ACC : MUT;
    v.threadLike = () => { const l = Object.assign({}, s.liked); l['n0'] = !l['n0']; this.setState({ liked: l }); };

    // ---------- COMPOSE
    v.audLabel = s.aud; v.audIsInner = s.aud === 'Inner circle';
    v.audToggle = () => this.setState({ aud: s.aud === 'Friends' ? 'Inner circle' : 'Friends' });
    v.audNote = s.aud === 'Friends' ? 'All your friends can see this.' : 'Only your ' + s.inner.length + ' inner-circle friends can see this.';
    v.editCircle = () => this.go('circle');
    v.doPost = () => { const t = (s.draft || '').trim(); if (!t) return; this.setState({ posts: [{ id:'u' + s.posts.length, ini:'S', when:'now', text:t, tag: s.aud === 'Inner circle' ? 'Inner circle only' : '' }].concat(s.posts), draft:'', screen:'home', stack:[], homeTab:0 }); };
    v.canPost = (s.draft || '').trim().length > 0; v.postO = v.canPost ? 1 : 0.4;

    // ---------- GROUP
    const G = GROUPS[s.gSel], isMem = s.gMember[s.gSel], req = s.gReq[s.gSel];
    v.gName = G.t; v.gLetter = G.l; v.gColor = G.c; v.gMeta = G.topic.charAt(0) + G.topic.slice(1).toLowerCase() + ' \u00b7 ' + (G.members + (isMem && s.gSel !== 0 ? 0 : 0)) + ' members';
    v.gIsMember = isMem; v.gNotMember = !isMem && !req; v.gPending = !isMem && req;
    v.gTabPosts = s.gTab === 0; v.gTabPeople = s.gTab === 1;
    v.gpLine = s.gTab === 0 ? ACC : 'transparent'; v.gpeLine = s.gTab === 1 ? ACC : 'transparent';
    v.gpColor = s.gTab === 0 ? TXT : MUT; v.gpeColor = s.gTab === 1 ? TXT : MUT;
    v.gSetPosts = () => this.setState({ gTab: 0 }); v.gSetPeople = () => this.setState({ gTab: 1 });
    v.gRequest = () => { const r = s.gReq.slice(); r[s.gSel] = true; this.setState({ gReq: r }); };
    v.gposts = [{ ini:'PK', when:'40m', text:'Golden hour shoot this Sunday. Meet at the cathedral steps, 6:30 sharp.' }, { ini:'TN', when:'3h', text:'Finally got the long-exposure shot of the fountain right.' }].map(g => { const p = PEOPLE[g.ini]; return { ini:g.ini, name:p[0], col:p[1], fg:fgOf(p[1]), whenText:'\u00b7 ' + g.when, text:g.text }; });
    const gn = [];
    [['PK',-90],['JL',90]].forEach(h => { const pos = sinpos(160 > 0 ? 195 : 0, 156, 58, h[1] + 90); const p = PEOPLE[h[0]]; gn.push({ x:(pos[0]-19).toFixed(1), y:(pos[1]-19).toFixed(1), s:38, ini:h[0], col:p[1], fg:fgOf(p[1]), ring:true, dashed:false, more:false }); });
    const ring = ['AO','TN','RS','EK','MD','REQ1','SY','NB','LF','REQ2','OH','MORE'];
    ring.forEach((it, k) => { const a = -90 + 15 + k * 30, x = 195 + 118*Math.cos(a*Math.PI/180), y = 156 + 118*Math.sin(a*Math.PI/180);
      if (it === 'REQ1' || it === 'REQ2') gn.push({ x:(x-18).toFixed(1), y:(y-18).toFixed(1), s:36, ini: it === 'REQ1' ? 'EV' : 'CZ', col:BG, fg:ACC, ring:false, dashed:true, more:false });
      else if (it === 'MORE') gn.push({ x:(x-18).toFixed(1), y:(y-18).toFixed(1), s:36, ini:'+117', col:TRACK, fg:MUT, ring:false, dashed:false, more:true });
      else { const p = PEOPLE[it]; gn.push({ x:(x-18).toFixed(1), y:(y-18).toFixed(1), s:36, ini:it, col:p[1], fg:fgOf(p[1]), ring:false, dashed:false, more:false }); } });
    v.gnodes = gn.map(n => Object.assign({}, n, { bd: n.dashed ? '2px dashed ' + ACC : 'none', sh: n.ring ? '0 0 0 2px ' + BG + ', 0 0 0 4px ' + ACC : '0 0 0 2px ' + BG, fs: n.more ? 10.5 : 12 }));

    // ---------- EVENT
    const E = EVENTS[s.eSel], me2 = s.joined[s.eSel], going = E.going + (me2 ? 1 : 0);
    v.eTitle = E.t; v.eVis = E.vis; v.eWhen = E.when + ' \u00b7 ' + E.where; v.eHost = PEOPLE[E.host][0];
    v.eCount = going; v.eCap = '/' + E.cap; v.eLeft = E.cap - going; v.eJoined = me2; v.eNotJoined = !me2; v.eFull = going >= E.cap && !me2;
    const seats = [], N = E.cap, size = N <= 12 ? 38 : 30;
    for (let k = 0; k < N; k++) {
      const a = (-90 + k * 360 / N) * Math.PI / 180, x = 195 + 108*Math.cos(a), y = 150 + 108*Math.sin(a);
      let kind = 'empty', ini = '', col = BG, fg = MUT, ring = false;
      if (k < E.going) { kind = 'person'; ini = k === 0 ? E.host : SEATPOOL[k]; const p = PEOPLE[ini]; col = p[1]; fg = fgOf(p[1]); ring = k === 0; }
      else if (k === E.going && me2) { kind = 'me'; ini = 'S'; col = PEOPLE.S[1]; fg = '#F1F0EC'; ring = true; }
      seats.push({ x:(x-size/2).toFixed(1), y:(y-size/2).toFixed(1), s:size, ini, col, fg, filled: kind !== 'empty', bd: kind === 'empty' ? '2px dashed ' + MUT : 'none', op: kind === 'empty' ? 0.6 : 1,
        sh: ring ? '0 0 0 2px ' + BG + ', 0 0 0 4px ' + ACC : '0 0 0 2px ' + BG, fs: size === 38 ? 12 : 10 });
    }
    v.seats = seats;
    v.eJoin = () => { const j = s.joined.slice(); j[s.eSel] = true; this.setState({ joined: j }); };
    v.eLeave = () => { const j = s.joined.slice(); j[s.eSel] = false; this.setState({ joined: j }); };
    return v;
  }
}

