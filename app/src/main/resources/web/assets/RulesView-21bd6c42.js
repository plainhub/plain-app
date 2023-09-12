import{d as le,C as ce,r as V,u as se,a3 as B,am as R,b$ as F,c0 as re,ai as ue,J as pe,aj as N,o as r,c as u,e,g as o,j as c,O as _,c1 as T,c2 as O,F as w,A as C,P as Y,ak as Z,x as A,y as x,l as J,ap as me,i as _e,t as ve,a7 as L,c3 as fe,c4 as he,N as ge,R as ee,f as te,S as oe,w as ne,X as j,a8 as $e,$ as be}from"./index-df35a132.js";import{_ as ke}from"./Breadcrumb-120bdd32.js";import{T as $,a as b,_ as ye,A as we}from"./question-mark-rounded-3eeccb5f.js";import{u as Ce,a as Fe}from"./vee-validate.esm-aa1410cb.js";import"./stringToArray-a8dcbce9.js";const Ne={slot:"headline"},Te={slot:"content"},Ve={class:"row"},Ae={class:"col-md-3 col-form-label"},De={class:"col-md-9 form-checks"},Ie={class:"form-check form-check-inline"},Me={class:"form-check-label",for:"action-allow"},Se={class:"form-check form-check-inline"},Ee={class:"form-check-label",for:"action-block"},Ue={class:"row mb-2"},qe={for:"action",class:"col-md-3 col-form-label"},Re={class:"col-md-9 form-checks"},Oe={class:"form-check form-check-inline"},Je={class:"form-check-label",for:"direction-inbound"},Le={class:"form-check form-check-inline"},je={class:"form-check-label",for:"direction-outbound"},Be={class:"row mb-3"},Qe={class:"col-md-3 col-form-label"},Ge={class:"col-md-9"},Pe=["value"],Xe={key:0,class:"input-group mt-2"},ze=["placeholder"],He={class:"inner"},Ke={class:"help-block"},We={value:""},Ye=["value"],Ze={key:2,class:"invalid-feedback"},xe={class:"row mb-3"},et={class:"col-md-3 col-form-label"},tt={class:"col-md-9"},ot={value:"all"},nt=["value"],at=["value"],lt={class:"row mb-3"},st={class:"col-md-3 col-form-label"},it={class:"col-md-9"},dt={slot:"actions"},ct=["disabled"],ae=le({__name:"EditRuleModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(k){var y,Q,G,P,X,z,H;const f=k,{handleSubmit:g}=Ce(),l=ce({action:"block",direction:"inbound",protocol:"all",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=V($.DNS),D=Object.values($),{t:I}=se(),{mutate:M,loading:S,onDone:E}=B({document:R`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `,options:{update:(n,s)=>{re(n,s.data.createConfig,R`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${F}
        `)}}}),{mutate:U,loading:q,onDone:a}=B({document:R`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `}),{value:m,resetField:v,errorMessage:h}=Fe("inputValue",ue().test("required",n=>"valid.required",n=>!b.hasInput(p.value)||!!n).test("target-value",n=>"invalid_value",n=>b.isValid(p.value,n??""))),i=(y=f.data)==null?void 0:y.data;l.action=(i==null?void 0:i.action)??"block",l.direction=(i==null?void 0:i.direction)??"inbound",l.protocol=(i==null?void 0:i.protocol)??"all",p.value=((G=(Q=f.data)==null?void 0:Q.target)==null?void 0:G.type)??$.DNS,m.value=((X=(P=f.data)==null?void 0:P.target)==null?void 0:X.value)??"",l.apply_to=((H=(z=f.data)==null?void 0:z.applyTo)==null?void 0:H.toValue())??"all",l.notes=(i==null?void 0:i.notes)??"",l.is_enabled=(i==null?void 0:i.is_enabled)??!0,i||v(),pe(p,(n,s)=>{(n===$.INTERFACE||s===$.INTERFACE)&&(m.value="")});const d=g(()=>{const n=new b;n.type=p.value,n.value=m.value??"",l.target=n.toValue(),f.data?U({id:f.data.id,input:{group:"rule",value:JSON.stringify(l)}}):M({input:{group:"rule",value:JSON.stringify(l)}})});return E(()=>{N()}),a(()=>{N()}),(n,s)=>{var K,W;const ie=ye,de=me;return r(),u("md-dialog",null,[e("div",Ne,o(c(i)?n.$t("edit"):n.$t("create")),1),e("div",Te,[e("div",Ve,[e("label",Ae,o(n.$t("actions")),1),e("div",De,[e("div",Ie,[_(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-allow",value:"allow","onUpdate:modelValue":s[0]||(s[0]=t=>l.action=t)},null,512),[[T,l.action]]),e("label",Me,o(n.$t("allow")),1)]),e("div",Se,[_(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-block",value:"block","onUpdate:modelValue":s[1]||(s[1]=t=>l.action=t)},null,512),[[T,l.action]]),e("label",Ee,o(n.$t("block")),1)])])]),e("div",Ue,[e("label",qe,o(n.$t("direction")),1),e("div",Re,[e("div",Oe,[_(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-inbound",value:"inbound","onUpdate:modelValue":s[2]||(s[2]=t=>l.direction=t)},null,512),[[T,l.direction]]),e("label",Je,o(n.$t("inbound")),1)]),e("div",Le,[_(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-outbound",value:"outbound","onUpdate:modelValue":s[3]||(s[3]=t=>l.direction=t)},null,512),[[T,l.direction]]),e("label",je,o(n.$t("outbound")),1)])])]),e("div",Be,[e("label",Qe,o(n.$t("match")),1),e("div",Ge,[_(e("select",{class:"form-select","onUpdate:modelValue":s[4]||(s[4]=t=>p.value=t)},[(r(!0),u(w,null,C(c(D),t=>(r(),u("option",{value:t},o(n.$t(`target_type.${t}`)),9,Pe))),256))],512),[[O,p.value]]),c(b).hasInput(p.value)?(r(),u("div",Xe,[_(e("input",{type:"text",class:"form-control","onUpdate:modelValue":s[5]||(s[5]=t=>Z(m)?m.value=t:null),placeholder:n.$t("for_example")+" "+c(b).hint(p.value)},null,8,ze),[[Y,c(m)]]),A(de,{class:"input-group-text"},{content:x(()=>[e("pre",Ke,o(n.$t(`examples_${p.value}`)),1)]),default:x(()=>[e("span",He,[A(ie)])]),_:1})])):J("",!0),p.value===c($).INTERFACE?_((r(),u("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":s[6]||(s[6]=t=>Z(m)?m.value=t:null)},[e("option",We,o(n.$t("all_local_networks")),1),(r(!0),u(w,null,C((K=k.networks)==null?void 0:K.filter(t=>t.type!=="wan"),t=>(r(),u("option",{value:t.ifName},o(t.name),9,Ye))),256))],512)),[[O,c(m)]]):J("",!0),c(h)?(r(),u("div",Ze,o(c(h)?n.$t(c(h)):""),1)):J("",!0)])]),e("div",xe,[e("label",et,o(c(I)("apply_to")),1),e("div",tt,[_(e("select",{class:"form-select","onUpdate:modelValue":s[7]||(s[7]=t=>l.apply_to=t)},[e("option",ot,o(n.$t("all_devices")),1),(r(!0),u(w,null,C((W=k.networks)==null?void 0:W.filter(t=>t.type!=="wan"),t=>(r(),u("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,nt))),128)),(r(!0),u(w,null,C(k.devices,t=>(r(),u("option",{value:"mac:"+t.mac},o(t.name),9,at))),256))],512),[[O,l.apply_to]])])]),e("div",lt,[e("label",st,o(n.$t("notes")),1),e("div",it,[_(e("textarea",{class:"form-control","onUpdate:modelValue":s[8]||(s[8]=t=>l.notes=t),rows:"3"},null,512),[[Y,l.notes]])])])]),e("div",dt,[e("md-outlined-button",{value:"cancel",onClick:s[9]||(s[9]=(...t)=>c(N)&&c(N)(...t)),autofocus:""},o(n.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:c(S)||c(q),onClick:s[10]||(s[10]=(...t)=>c(d)&&c(d)(...t))},o(n.$t("save")),9,ct)])])}}}),rt={class:"page-container"},ut={class:"main"},pt={class:"v-toolbar"},mt={class:"table-responsive"},_t={class:"table"},vt=e("th",null,"ID",-1),ft={class:"actions two"},ht={class:"form-check"},gt=["disabled","onChange","checked"],$t={class:"nowrap"},bt={class:"nowrap"},kt={class:"actions two"},yt=["onClick"],wt=["onClick"],At=le({__name:"RulesView",setup(k){const f=V([]),g=V([]),l=V([]),{t:p}=se();_e({handle:(a,m)=>{m?ve(p(m),"error"):(f.value=a.configs.filter(v=>v.group==="rule").map(v=>{const h=JSON.parse(v.value),i=new we;i.parse(h.apply_to);const d=new b;return d.parse(h.target),{id:v.id,createdAt:v.createdAt,updatedAt:v.updatedAt,data:h,applyTo:i,target:d}}),g.value=[...a.devices],l.value=[...a.networks])},document:L`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${fe}
    ${F}
    ${he}
  `});const D=L`
  mutation DeleteConfig($id: ID!) {
    deleteConfig(id: $id)
  }
`;function I(a){j($e,{id:a.id,name:a.id,gql:D,appApi:!1,typeName:"Config"})}function M(a){j(ae,{data:a,devices:g,networks:l})}function S(){j(ae,{data:null,devices:g,networks:l})}const{mutate:E,loading:U}=B({document:L`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${F}
  `});function q(a){E({id:a.id,input:{group:"rule",value:JSON.stringify(a.data)}})}return(a,m)=>{const v=ke,h=be,i=ge("tooltip");return r(),u("div",rt,[e("div",ut,[e("div",pt,[A(v,{current:()=>a.$t("page_title.rules")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:S},o(a.$t("create")),1)]),e("div",mt,[e("table",_t,[e("thead",null,[e("tr",null,[vt,e("th",null,o(a.$t("apply_to")),1),e("th",null,o(a.$t("description")),1),e("th",null,o(a.$t("notes")),1),e("th",null,o(a.$t("enabled")),1),e("th",null,o(a.$t("created_at")),1),e("th",null,o(a.$t("updated_at")),1),e("th",ft,o(a.$t("actions")),1)])]),e("tbody",null,[(r(!0),u(w,null,C(f.value,d=>(r(),u("tr",{key:d.id},[e("td",null,[A(h,{id:d.id,raw:d.data},null,8,["id","raw"])]),e("td",null,o(d.applyTo.getText(a.$t,g.value,l.value)),1),e("td",null,o(a.$t(`rule_${d.data.direction}`,{action:a.$t(d.data.action),target:d.target.getText(a.$t,l.value)})),1),e("td",null,o(d.data.notes),1),e("td",null,[e("div",ht,[e("md-checkbox",{"touch-target":"wrapper",disabled:c(U),onChange:y=>q(d),checked:d.data.is_enabled},null,40,gt)])]),e("td",$t,[_((r(),u("span",null,[te(o(c(oe)(d.createdAt)),1)])),[[i,c(ee)(d.createdAt)]])]),e("td",bt,[_((r(),u("span",null,[te(o(c(oe)(d.updatedAt)),1)])),[[i,c(ee)(d.updatedAt)]])]),e("td",kt,[e("a",{href:"#",class:"v-link",onClick:ne(y=>M(d),["prevent"])},o(a.$t("edit")),9,yt),e("a",{href:"#",class:"v-link",onClick:ne(y=>I(d),["prevent"])},o(a.$t("delete")),9,wt)])]))),128))])])])])])}}});export{At as default};
